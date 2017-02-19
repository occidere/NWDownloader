package util;

import java.io.File;

public class CMD {
	
	//디폴트 폴더
	private static final String DEFAULT_PATH = "C:\\Webtoon\\";
	
	/**
	 * 폴더 생성 메서드(static way).
	 * 매개변수가 없으면 디폴트 폴더 생성
	 * (C:\Webtoon\)
	 */
	public static void makeDir(){
		makeDir(DEFAULT_PATH);
	}
	
	/**
	 * 폴더 생성 메서드(static way)
	 * @param path 생성할 폴더 주소
	 */
	public static void makeDir(String path){
		File f = new File(path);
		if(!f.exists()) f.mkdirs();
	}
	
	/**
	 * 디폴트 폴더 열기 메서드. 윈도우에서만 작동.
	 * 매개변수로 DEFAULT_PATH를 전달한다.
	 */
	public static void openFolder(){
		openFolder(DEFAULT_PATH);
	}
	
	/**
	 * 특정 폴더 열기 메서드. 윈도우에서만 작동.
	 * 우선 폴더를 생성 후, 열기 커맨드 실행
	 */
	public static void openFolder(String path){
		makeDir(path);
		try{
			Runtime.getRuntime().exec("explorer.exe "+DEFAULT_PATH);
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}