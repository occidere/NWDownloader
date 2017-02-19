package downloader;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import util.CMD;

import downloader.Error;
import downloader.Preprocess;

public class Downloader {
	
	//기본 저장경로
	private final String DEFAULT_PATH = "C:\\Webtoon\\";
	private Preprocess preprocess; //전처리 객체
	
	private Map<String, String> cookies; //로그인 성공시 쿠키 담김
	
	//싱글톤 패턴
	private static Downloader instance;
	private Downloader(){
		preprocess = new Preprocess(); //전처리 클래스 생성자에서 객체생성
	}
	public static Downloader getInstance(){
		if(instance == null) instance = new Downloader();
		return instance;
	}
	
	/**
	 * 쿠키 등록 메서드.
	 * @param cookies 로그인 메서드에서 정상적인 로그인 후 리턴되는 쿠키값
	 */
	public void setCookies(Map<String, String> cookies){
		this.cookies = cookies;
	}
	
	/**
	 * 쿠키 제거 메서드
	 */
	public void removeCookies(){
		cookies = null;
	}
	
	/**
	 * 다운로드 메서드
	 * @param address 웹툰 주소
	 * @param path 저장경로
	 * @param imgUrl 이미지 url주소
	 * @param pageNum 페이지번호
	 */
	private void download(String address, String path, String imgUrl, int pageNum) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(path + String.format("%03d", ++pageNum) + preprocess.getExt(imgUrl));
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
		catch (Exception e) { Error.printErrMsg(0);}
	}
	
	/**
	 * (오버로딩) 여러편 다운로드용 메서드.
	 * 두 주소가 들어오면 회차수 구하기 등의 전처리 과정을 거친 뒤,
	 * 매개변수가 1개짜리인 getConnection메서드로 보냄.
	 * 전처리 과정 중 시작주소와 끝 주소의 대소판별 기능이 있으므로
	 * 시작주소와 끝 주소의 순서는 딱히 상관 없다.
	 * @param startAddr 시작 주소
	 * @param endAdder 끝 주소
	 */
	public void getConnection(String startAddr, String endAdder){
		String prefix = "no=";
		String address = startAddr.substring(0, startAddr.indexOf(prefix)+prefix.length());
		
		//시작주소와 끝 주소가 서로 다른 만화일 경우
		if(!preprocess.areSameComic(startAddr, endAdder)){
			Error.printErrMsg(2);
			return;
		}

		//만화 회차수 구하기
		int startNo = preprocess.getNo(startAddr), endNo = preprocess.getNo(endAdder);
		
		//시작주소 넘버가 마지막주소보다 클 경우 서로 바꿔준다.
		if(startNo > endNo){
			int tmp =  startNo;
			startNo = endNo;
			endNo = tmp;
		}
		
		System.out.printf("총 회차수 %d개\n", (endNo-startNo+1));
		
		//실패시 자동 다음화 다운로드 시도
		for(int i=startNo;i<=endNo;i++) getConnection(address+i);
	}
	
	
	/**
	 * JSoup을 이용하여 만화 이미지 주소를 파싱 & download메서드로 넘겨주는 역할하는 메서드.
	 * @param address 만화 주소
	 */
	public void getConnection(String address) {
		//제대로 된 주소인지 검증
		if(!address.contains("http://comic.naver.com/webtoon/detail.nhn?titleId")){
			Error.printErrMsg(1);
			return;
		}
		
		try{
			//타임아웃 60초. 쿠키가 있으면 쿠키 전달
			Connection conn = Jsoup.connect(address).userAgent("Mozilla/5.0").timeout(60000);
			if(cookies!=null) conn.cookies(cookies);
			
			Document doc = conn.get();
			
			//parentTitle은 최상위 폴더로 지정할 웹툰 제목 & 특수문자 제거 & 정규식 수정
			String parentTitle = doc.select("h2.ly_tit").text().replaceAll("[\\/:*?<>|.]", " ").trim();
			
			//title은 회차수 까지 포함한 최상위 폴더의 내부에 생성될 개별 폴더 & 특수문자 제거 & 정규식 수정
			String title = doc.select("meta[property=og:title]").attr("content").replaceAll("[\\/:*?<>|.]", " ").trim();
			
			//연령확인, 구매확인 등 로그인이 필요한 웹툰인 경우 div class="title"에 "로그인후" 시도하라는 메세지가 담긴다.
			String loginTitle = doc.select("div.title").text();
			
			if(loginTitle.contains("로그인후")){
				Error.printErrMsg(4); //로그인이 필요한 경우]
				return;
			}
			else if(parentTitle.equals("") || title.equals("네이버웹툰")){
				Error.printErrMsg(3); //없는 만화일 경우 종료
				return;
			}
			
			//path는 최종 다운로드 주소
			String path = DEFAULT_PATH + parentTitle + "\\" + title + "\\";
			
			int pageNum = 0;
			
			System.out.printf("제목 : %s\n다운로드 폴더 : %s\n", title, path);
			
			//파일 다운받을 경로 생성 ex)C:/webtoon/복학왕/복학왕 - 115화/
			CMD.makeDir(path);
			
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
		catch(Exception e){ Error.printErrMsg(0); }
	}
	
	public void close(){
		cookies = null;
		preprocess = null;
		instance = null;
	}
}