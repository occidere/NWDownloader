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
	private static boolean status = true; //실패 여부 변수
	public static void main(String[] args) throws Exception {
		Scanner sc;
		while(true){
			int menuSelect, innerSelector;
			sc = new Scanner(System.in);
			System.out.println("메뉴를 선택하세요\n  1. 한 편씩 다운로드\n  2. 여러 편씩 다운로드\n  3. 다운로드 폴더 열기\n  0. 종료"); menuSelect = sc.nextInt();
			switch(menuSelect){
				case 1:{
					innerSelector = 1;
					while(true){
						sc = new Scanner(System.in);
						System.out.print("주소를 입력하세요 : ");{
							connector(sc.nextLine());
							//실패시 메뉴로 돌아가기
							if(!status){
								System.out.println("다운로드 실패! 메뉴로 돌아갑니다.\n"); break;
							}
						}
						System.out.print("종료는 0, 다른 만화 다운로드는 1을 입력하세요 : "); innerSelector = sc.nextInt();
						if(innerSelector == 0) break;
					}
					break;
				}
				case 2:{
					innerSelector = 1;
					while(true){
						sc = new Scanner(System.in);
						int s, e;
						String start, end, address;
						System.out.print("시작할 주소를 입력하세요 : "); start = sc.nextLine();
						System.out.print("마지막 주소를 입력하세요 : "); end = sc.nextLine();
						
						address = start.substring(0, start.indexOf("no="))+"no=";
						
						s = Integer.parseInt(start.substring(start.indexOf("no=")+3, start.indexOf("&week")));
						e = Integer.parseInt(end.substring(end.indexOf("no=")+3, end.indexOf("&week")));
						
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
							if(!status) System.out.println("다운로드 실패! 다음화 다운로드를 시도합니다...");
						}
						
						System.out.print("종료는 0, 다른 만화 다운로드는 1을 입력하세요 : "); innerSelector = sc.nextInt();
						if(innerSelector == 0) break;
					}
					break;
				}
				case 3:{
					// C:/Webtoon/ 폴더 열기
					Runtime.getRuntime().exec("explorer.exe "+defaultPath);
					break;
				}
				case 0: System.out.println("프로그램을 종료합니다.");
				default: sc.close(); return;
			}
		}
	}
	private static void connector(String address) {
		try{
			//타임아웃 30초
			Document doc = Jsoup.connect(address).userAgent("Mozilla/5.0").timeout(30000).get();
			
			//parentTitle은 최상위 폴더로 지정할 웹툰 제목 & 특수문자 제거 & 정규식 수정
			String parentTitle = doc.select("h2.ly_tit").text().replaceAll("[\\/:*?<>|.]", " ").trim();
			//title은 회차수 까지 포함한 최상위 폴더의 내부에 생성될 개별 폴더 & 특수문자 제거 & 정규식 수정
			String title = doc.select("meta[property=og:title]").attr("content").replaceAll("[\\/:*?<>|.]", " ").trim();
			//path는 최종 다운로드 주소
			String path = defaultPath+parentTitle+"\\"+title+"\\";
			int pageNum = 0;
			
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n",title,path);
			
			//파일 다운받을 경로 생성 ex)C:/webtoon/복학왕/복학왕 - 115화/
			File f = new File(path);
			f.mkdirs();
			
			//<img src= 부분 파싱
			Elements elements = doc.select("img[src~=imgcomic]");
			//전체 파일 개수
			int total = elements.size();
			System.out.printf("다운로드 시작 (전체 %d개)\n", total);
			String imgUrl = "";
			for(Element e : elements){
				imgUrl = e.attr("src");
				download(address, path, imgUrl, pageNum);
				
				if(!status) return;
				
				System.out.printf("%2d / %2d ...... 완료!\n", ++pageNum, total);
			}
			status = true;
		}
		catch(Exception e){
			status = false;
			return;
		}
	}
	
	private static void download(String address, String path, String imgUrl, int pageNum) {
		String extension = "", preNum = "";
		
		//확장자 판단
		if (imgUrl.contains("jpg")) extension = "jpg";
		else if (imgUrl.contains("jpeg")) extension = "jpeg";
		else if (imgUrl.contains("png")) extension = "png";
		else if (imgUrl.contains("gif")) extension = "gif";
		else if (imgUrl.contains("bmp")) extension = "bmp";
		
		// 페이지 번호 설정. 001.jpg, 015.jpg, 257.jpg 이런식으로 3자리수까지 오름차순 저장 가능
		if(pageNum+1<10) preNum = "00";
		else if(pageNum+1<100) preNum = "0";
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(path+preNum+(pageNum+1)+"."+extension);
			HttpURLConnection conn = (HttpURLConnection)new URL(imgUrl).openConnection();
			conn.setConnectTimeout(30000); //최대 30초까지 시간 지연 기다려줌
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Referer", address); //핵심! 이거 빠지면 연결 안됨
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			InputStream in = conn.getInputStream();
			
			status = true;
			
			//다운로드 부분. 버퍼 크기 32*1024B(32Kb)로 조정
			byte[] buf = new byte[32768];
			int len = 0;
			while((len = in.read(buf))>0) fos.write(buf, 0, len);
			fos.close();
		} 
		catch (Exception e) {
			status = false;
			return;
		}
	}
}
