package parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NWDownloader {
	private static final String defaultPath = "C:\\Webtoon\\";
	private static boolean status; //실패 여부 변수. true = 정상, false = 실패
	
	public static void main(String[] args) throws Exception {
		Scanner sc;
		while(true){
			int selector;
			sc = new Scanner(System.in);
			System.out.println("메뉴를 선택하세요\n  1. 한 편씩 다운로드\n  2. 여러 편씩 다운로드\n  3. 다운로드 폴더 열기\n  0. 종료"); selector = sc.nextInt();
			switch(selector){
				case 1:{
					while(true){
						sc = new Scanner(System.in);
						System.out.print("주소를 입력하세요 : "); connector(sc.nextLine());
						
						// 실패시 메뉴로 돌아가기
						printErrMsg(isStatus());
						
						System.out.print("종료는 0, 다른 만화 다운로드는 1을 입력하세요 : "); if(sc.nextInt() == 0) break;
					}
					break;
				}
				case 2:{
					while(true){
						sc = new Scanner(System.in);
						int s, e;
						String start, end, address;
						System.out.print("시작할 주소를 입력하세요 : "); start = sc.nextLine();
						System.out.print("마지막 주소를 입력하세요 : "); end = sc.nextLine();
						
						address = start.substring(0, start.indexOf("no="))+"no=";
						
						//만화 회차수 구하기
						s = getNo(start);
						e = getNo(end);
						
						//시작주소와 끝 주소가 서로 다른 만화일 경우
						if(!areSameComic(start, end)) break;
						
						//시작주소 넘버가 마지막주소보다 클 경우 서로 바꿔준다.
						if(s > e){
							int tmp = s;
							s = e;
							e = tmp;
						}
						System.out.printf("총 회차수 %d개\n", (e-s+1));
						
						//실패시 자동 다음화 다운로드 시도
						for(int i=s;i<=e;i++){
							connector(address+i);
							printErrMsg(isStatus());
						}
						
						System.out.print("종료는 0, 다른 만화 다운로드는 1을 입력하세요 : "); if(sc.nextInt() == 0) break;
					}
					break;
				}
				case 3:{
					// C:/Webtoon/ 폴더 열기. 폴더가 없는 경우 먼저 생성
					makeDir(defaultPath);
					Runtime.getRuntime().exec("explorer.exe "+defaultPath);
					break;
				}
				case 0: System.out.println("프로그램을 종료합니다.");
				default: sc.close(); return;
			}
		}
	}
	
	//status에 따라 메세지 출력
	private static void printErrMsg(boolean status){
		if(!status) System.out.println("다운로드 실패!");
	}
	
	//만화 회차수 구하는 메서드
	private static int getNo(String addr){
		return Integer.parseInt(addr.substring(addr.indexOf("no=")+3, addr.indexOf("&week")));
	}
	
	//status 상태를 반환하는 getter메서드
	private static boolean isStatus() {
		return status;
	}

	//status를 변경해주는 setter메서드를 이용해 private 멤버 직접참조 막음
	private static void setStatus(boolean status) {
		NWDownloader.status = status;
	}

	//폴더 생성 메서드
	private static File makeDir(String path){
		File f = new File(path);
		if(!f.exists()) f.mkdirs();
		return f;
	}

	//시작주소와 끝 주소가 서로 다른 만화일 경우 검증용
	private static boolean areSameComic(String start, String end){
		if(!start.substring(0, start.indexOf("&no=")).equals(end.substring(0, end.indexOf("&no=")))){
			System.out.println("같은 만화의 주소를 입력해 주세요.");
			return false;
		}
		else return true;
	}
	
	//만화 주소와 연결해주는 메서드
	private static void connector(String address) {
		setStatus(true); //처음엔 무조건 true 상태로 시작
		
		//제대로 된 주소인지 검증
		if(!address.contains("http://comic.naver.com/webtoon/detail.nhn?titleId")){
			System.out.println("잘못된 주소입니다.");
			setStatus(false);
			return;
		}
		
		try{
			//타임아웃 30초
			Document doc = Jsoup.connect(address).userAgent("Mozilla/5.0").timeout(30000).get();
			
			//parentTitle은 최상위 폴더로 지정할 웹툰 제목 & 특수문자 제거 & 정규식 수정
			String parentTitle = doc.select("h2.ly_tit").text().replaceAll("[\\/:*?<>|.]", " ").trim();
			//title은 회차수 까지 포함한 최상위 폴더의 내부에 생성될 개별 폴더 & 특수문자 제거 & 정규식 수정
			String title = doc.select("meta[property=og:title]").attr("content").replaceAll("[\\/:*?<>|.]", " ").trim();
			
			//없는 만화일 경우 종료
			if(parentTitle.equals("")||title.equals("네이버웹툰")){
				setStatus(false);
				return;
			}
			
			//path는 최종 다운로드 주소
			String path = defaultPath+parentTitle+"\\"+title+"\\";
			int pageNum = 0;
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n",title,path);
			
			//파일 다운받을 경로 생성 ex)C:/webtoon/복학왕/복학왕 - 115화/
			makeDir(path);
			
			//<img src= 부분 파싱
			Elements elements = doc.select("img[src~=imgcomic]");
			//전체 파일 개수
			int total = elements.size();
			System.out.printf("다운로드 시작 (전체 %d개)\n", total);
			//String imgUrl = "";
			for(Element e : elements){
				//imgUrl = e.attr("src");
				download(address, path, e.attr("src"), pageNum);
				System.out.printf("%2d / %2d ...... 완료!\n", ++pageNum, total);
			}
		}
		catch(Exception e){
			setStatus(false);
			return;
		}
	}
	
	//확장자 설정 메서드
	private static String setExt(String imgUrl){
		String ext="";
		for(int i=imgUrl.length()-1;i>=0;i--){
			ext = imgUrl.charAt(i) + ext;
			if (imgUrl.charAt(i)=='.') break;
		}
		return ext;
	}
	
	//페이지 번호 완성 메서드. 001.jpg, 015.jpg, 257.jpg 이런식으로 3자리수까지 오름차순 저장 가능
	private static String setPageNum(int pageNum){
		String preNum="";
		if(pageNum+1<10) preNum = "00";
		else if(pageNum+1<100) preNum = "0";
		return preNum+(pageNum+1);
	}
	
	//다운로드 메서드
	private static void download(String address, String path, String imgUrl, int pageNum) {
		if(!isStatus()) return;
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(path + setPageNum(pageNum) + setExt(imgUrl));
			HttpURLConnection conn = (HttpURLConnection)new URL(imgUrl).openConnection();
			conn.setConnectTimeout(30000); //최대 30초까지 시간 지연 기다려줌
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Referer", address); //핵심! 이거 빠지면 연결 안됨
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			InputStream in = conn.getInputStream();
			
			//다운로드 부분. 버퍼 크기 32*1024B(32Kb)로 조정
			byte[] buf = new byte[32768];
			int len = 0;
			while((len = in.read(buf))>0) fos.write(buf, 0, len);
			fos.close();
		} 
		catch (Exception e) {
			setStatus(false);
			return;
		}
	}
}
