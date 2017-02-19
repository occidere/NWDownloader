package downloader;

/**
 * Downloader.java 클래스에서만 사용될 전처리용 클래스.
 * 따라서 모든 메서드는 package access이다.
 * @author occidere
 */
public class Preprocess {
	
	/**
	 * 확장자 설정 메서드. 뒤에서부터 . 을 만날때 까지 차례로 탐색
	 * @param imgUrl 이미지 확장자가 들어간 이미지파일의 String 타입 주소
	 * @return String 타입의 .을 포함한 이미지 확장자 (.jpg 등)
	 */
	String getExt(String imgUrl){
		String ext="";
		int size = imgUrl.length();
		while(size-->-1 && imgUrl.charAt(size)!='.') 
			ext = imgUrl.charAt(size)+ext;
		return "."+ext;
	}
	
	/**
	 * 만화 회차수 구하는 메서드. 단순히 no= ~ &week 사이의 숫자를 형변환해서 리턴
	 * http://comic.naver.com/webtoon/detail.nhn?titleId=651617&no=96&weekday=sun에서
	 * 96에 해당
	 * @param addr 만화 주소
	 * @return 만화 회차수 int 값
	 */
	int getNo(String addr){
		String prefix = "no=", suffix = "&week";
		return Integer.parseInt(addr.substring(addr.indexOf(prefix)+prefix.length(), addr.indexOf(suffix)));
	}
	
	/**
	 * 여러 편 다운로드시 시작 주소와 끝 주소가 동일한 만화인지 아닌지 판별하는 메서드
	 * http://comic.naver.com/webtoon/detail.nhn?titleId=651617&no=96&weekday=sun에서
	 * 651617에 해당
	 * @param startAddr 시작 주소
	 * @param endAddr 끝 주소
	 * @return 서로 동일한 만화이면 true, 다르면 false
	 */
	boolean areSameComic(String startAddr, String endAddr){
		String prefix = "titleId=", suffix = "&no=", startTitleId, endTitleId;
		startTitleId = startAddr.substring(startAddr.indexOf(prefix)+prefix.length(), startAddr.indexOf(suffix));
		endTitleId = endAddr.substring(endAddr.indexOf(prefix)+prefix.length(), endAddr.indexOf(suffix));
		return startTitleId.equals(endTitleId) ? true : false;
	}
}
