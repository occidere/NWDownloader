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
	private static final String DEFAULT_PATH = "C:\\Webtoon\\";
	public static void main(String[] args) throws Exception {
		Scanner sc = new Scanner(System.in);
		int menuSelector = Integer.MAX_VALUE;
		System.out.println("제작자: occidere\t버전: 0.2.6");
		while(menuSelector!=0){
			System.out.println("메뉴를 선택하세요\n  1. 한 편씩 다운로드\n  2. 여러 편씩 다운로드\n  3. 다운로드 폴더 열기\n  0. 종료");
			menuSelector = sc.nextInt();
			switch(menuSelector){
				case 1:{
					System.out.print("주소를 입력하세요 : "); connector(sc.next().trim());
					break;
				}
				case 2:{
					String start, end, address;
					System.out.print("시작할 주소를 입력하세요 : "); start = sc.next().trim();
					System.out.print("마지막 주소를 입력하세요 : "); end = sc.next().trim();

					address = start.substring(0, start.indexOf("no=")+3);

					//만화 회차수 구하기
					int s = getNo(start), e = getNo(end);

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
					for(int i=s;i<=e;i++) connector(address+i);
					break;
				}
				case 3:{
					// C:/Webtoon/ 폴더 열기. 폴더가 없는 경우 먼저 생성
					makeDir(DEFAULT_PATH);
					Runtime.getRuntime().exec("explorer.exe "+DEFAULT_PATH);
					break;
				}
				//종료시 break문이 없어 자동으로 default까지 가서 스캐너 닫고 return으로 전체종료
				case 0: System.out.println("프로그램을 종료합니다.");
			}
		}
		sc.close();
	}
	
	//errorCode에 따라 메세지 출력
	private static void printErrMsg(int errorCode){
		StringBuilder errMsg = new StringBuilder().append("다운로드 실패: ");
		switch(errorCode){
			case 1:
				errMsg.append("잘못된 주소입니다.");
				break;
			case 2:
				errMsg.append("같은 만화의 주소를 입력해 주세요.");
				break;
			case 3:
				errMsg.append("없는 만화입니다.");
				break;
			default:
				errMsg.append("알수 없는 에러.");
		}
		System.out.println(errMsg);
	}
	
	//만화 회차수 구하는 메서드
	private static int getNo(String addr){
		return Integer.parseInt(addr.substring(addr.indexOf("no=")+3, addr.indexOf("&week")));
	}

	//폴더 생성 메서드
	private static void makeDir(String path){
		File f = new File(path);
		if(!f.exists()) f.mkdirs();
	}

	//시작주소와 끝 주소가 서로 다른 만화일 경우 검증용
	private static boolean areSameComic(String start, String end){
		if(!start.substring(0, start.indexOf("&no=")).equals(end.substring(0, end.indexOf("&no=")))){
			printErrMsg(2);
			return false;
		}
		return true;
	}
	
	//만화 주소와 연결해주는 메서드
	private static void connector(String address) {
		//제대로 된 주소인지 검증
		if(!address.contains("http://comic.naver.com/webtoon/detail.nhn?titleId")){
			printErrMsg(1);
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
				printErrMsg(3);
				return;
			}
			
			//path는 최종 다운로드 주소
			String path = new StringBuilder(DEFAULT_PATH+parentTitle+"\\"+title+"\\").toString();
			int pageNum = 0;
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n",title,path);
			
			//파일 다운받을 경로 생성 ex)C:/webtoon/복학왕/복학왕 - 115화/
			makeDir(path);
			
			//<img src= 부분 파싱
			Elements elements = doc.select("img[src~=imgcomic]");
			//전체 파일 개수
			int total = elements.size();
			System.out.printf("다운로드 시작 (전체 %d개)\n", total);
			for(Element e : elements){
				download(address, path, e.attr("src"), pageNum);
				System.out.printf("%3d / %3d ...... 완료!\n", ++pageNum, total);
			}
		}
		catch(Exception e){ printErrMsg(0); }
	}
	
	//확장자 설정 메서드
	private static String setExt(String imgUrl){
		String ext="";
		int size = imgUrl.length();
		while(size-->-1 && imgUrl.charAt(size)!='.') 
			ext = imgUrl.charAt(size)+ext;
		return "."+ext;
	}
	
	//다운로드 메서드
	private static void download(String address, String path, String imgUrl, int pageNum) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(path + String.format("%03d", ++pageNum) + setExt(imgUrl));
			HttpURLConnection conn = (HttpURLConnection)new URL(imgUrl).openConnection();
			conn.setConnectTimeout(60000); //최대 60초까지 시간 지연 기다려줌
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Referer", address); //핵심! 이거 빠지면 연결 안됨
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			InputStream in = conn.getInputStream();

			//다운로드 부분. 버퍼 크기 1024*1024B(1024Kb)로 조정
			byte[] buf = new byte[1024*1024];
			int len = 0;
			while((len = in.read(buf))>0) fos.write(buf, 0, len);
			fos.close();
		} 
		catch (Exception e) { printErrMsg(0); }
	}
}
