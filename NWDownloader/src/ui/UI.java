package ui;

import java.util.Scanner;

import authentication.ILogin;
import authentication.Login;

import downloader.Downloader;

import util.CMD;

public class UI {
	
	//제작자 출력. 수정 금지
	private UI(){ System.out.println("제작자: occidere\t버전: 0.3.0 (2017.02.19)"); }
	
	private static UI instance;
	public static UI getInstance(){
		if(instance==null) instance = new UI();
		return instance;
	}
	
	/**
	 * 화면 출력 메서드
	 */
	public void showMenu() {
		final int EXIT = 0; //종료는 0
		Scanner sc = new Scanner(System.in);
		
		//로그인용 세션
		ILogin loginService;
		//로그인 여부 판별
		boolean isLogin = false;
		
		Downloader downloader = Downloader.getInstance();
		
		int menuNum = Integer.MAX_VALUE;
		
		String startAddr, endAddr, id, pw;

		while (menuNum != EXIT) {
			System.out.println("메뉴를 선택하세요\n  1. 한 편씩 다운로드\n  2. 여러 편씩 다운로드\n  3. 다운로드 폴더 열기");
			System.out.println("  4. "+ (isLogin ? "로그아웃" : "로그인") + "\n  0. 종료");
			
			menuNum = sc.nextInt();

			switch (menuNum) {
			case 1:
				System.out.print("주소를 입력하세요: ");
				startAddr = sc.next().trim();
				downloader.getConnection(startAddr);
				break;

			case 2:
				System.out.print("시작할 주소를 입력하세요 : "); 
				startAddr = sc.next().trim();
				System.out.print("마지막 주소를 입력하세요 : "); 
				endAddr = sc.next().trim();
				
				downloader.getConnection(startAddr, endAddr);
				break;
			
			case 3:
				CMD.openFolder();
				break;
			
			case 4:
				if(isLogin){ //로그인이 된 상태
					isLogin = false;
					downloader.removeCookies();
					loginService = null;
					id = pw = null;
					System.out.println("로그아웃 성공!");
				}
				else{ //로그인이 안된 상태
					System.out.printf("아이디: ");
					id = sc.next();
					System.out.printf("비밀번호: ");
					pw = sc.next();
					
					try{
						//생성자를 통한 로그인 시도
						loginService = new Login(id, pw);
						if(loginService.isLogin()){
							isLogin = true;
							downloader.setCookies(loginService.getCookies());
						}
					}
					catch(Exception e){}
					finally{ id = pw = null; }
				}
				break;
				
			case 0:
				System.out.println("프로그램을 종료합니다");
				break;
			}
		}
		loginService = null;
		downloader.close();
		sc.close();
	}
	
	public void close(){
		instance = null;
	}
}