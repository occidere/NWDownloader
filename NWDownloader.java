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

	private static String address;
	private static File f;
	private static int pageNum;
	private static String extension;
	private static String preNum;
	
	public static void main(String[] args) throws Exception {
		Scanner sc = new Scanner(System.in);
		System.out.print("주소를 입력하세요 : "); address = sc.nextLine();

		Document doc = Jsoup.connect(address).userAgent("Mozilla/5.0").get();
		
		//parentTitle은 최상위 폴더로 지정할 웹툰 제목 & 특수문자 제거
		String parentTitle = doc.select("h2.ly_tit").text().replaceAll("[^[:alnum:]+]", " ");
		//title은 회차수 까지 포함한 최상위 폴더의 내부에 생성될 개별 폴더 & 특수문자 제거
		String title = doc.select("meta[property=og:title]").attr("content").replaceAll("[^[:alnum:]+]", " ");
		//path는 최종 다운로드 주소
		String path = "C:/Webtoon/"+parentTitle+"/"+title+"/";
		
		System.out.printf("제목 : %s\n다운로드 폴더 : %s\n",title,path);
		
		//파일 다운받을 경로 생성 ex)C:/webtoon/복학왕/복학왕 - 115화/
		f = new File(path);
		f.mkdirs();
		
		//<img src= 부분 파싱
		Elements elements = doc.select("img[src~=imgcomic]");
		//전체 파일 개수
		int total = elements.size();
		System.out.printf("다운로드 시작 (전체 %d개)\n", total);
		String imgUrl = "";
		for(Element e : elements){
			imgUrl = e.attr("src");
			download(path, imgUrl, pageNum);
			System.out.printf("%d / %d ...... 완료!\n", ++pageNum, total);
		}
		System.out.println("done!"); sc.next(); sc.close();
	}
	
	private static void download(String path, String imgUrl, int pageNum) throws Exception {
		
		//확장자 판단
		if (imgUrl.toString().contains("jpg")) extension = "jpg";
		else if (imgUrl.toString().contains("jpeg")) extension = "jpeg";
		else if (imgUrl.toString().contains("png")) extension = "png";
		else if (imgUrl.toString().contains("gif")) extension = "gif";
		else if (imgUrl.toString().contains("bmp")) extension = "bmp";
		
		// 페이지 번호 설정. 001.jpg, 015.jpg, 257.jpg 이런식으로 3자리수까지 오름차순 저장 가능
		if(pageNum+1<10) preNum = "00";
		else if(pageNum+1<100) preNum = "0";
		
		FileOutputStream fos = new FileOutputStream(path+preNum+(pageNum+1)+"."+extension);
		HttpURLConnection conn = (HttpURLConnection)new URL(imgUrl).openConnection();
		conn.setConnectTimeout(5000); //최대 5초까지 시간 지연 기다려줌
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Referer", address); //핵심! 이거 빠지면 연결 안됨
		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		InputStream in = conn.getInputStream();
		
		//다운로드 부분
		byte[] buf = new byte[2048];
		int len = 0;
		while((len = in.read(buf))>0) fos.write(buf, 0, len);
		fos.close();
	}
}
