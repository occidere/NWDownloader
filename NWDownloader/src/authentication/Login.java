package authentication;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlImageInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;

/**
 * 로그인 모듈
 * 알고리즘 참고: http://bugnote.tistory.com/48
 * @author occidere
 */
public class Login implements ILogin {
	//로그인 정보를 바탕으로 생성할 쿠키
	private static Map<String, String> cookies;
	//네이버 로그인 주소
	private static final String URL_LOGIN = "http://static.nid.naver.com/login.nhn";
	
	private boolean isLogin; //로그인 성공 = true, 실패 = false
	
	private WebClient webClient;
	private HtmlPage curPage;
	
	@Override
	public Map<String, String> getCookies() {
		// TODO Auto-generated method stub
		return makeLoginCookie();
	}
	
	@Override
	public boolean isLogin() {
		// TODO Auto-generated method stub
		return isLogin;
	}
	
	/**
	 * 생성자를 통해 로그인 시도
	 * @param id 입력한 네이버 id
	 * @param pw 입력한 네이버 pw
	 */
	public Login(String id, String pw) {
		
		System.out.println("로그인 시도중...");
		
		/* 필수! 로그 메세지 출력 안함 -> HtmlUnit 이용시 Verbose한 로그들이 너무 많아서 다 끔 */
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		
		//브라우저는 인터넷 익스플로러로 설정
		webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER);
		webClient.waitForBackgroundJavaScript(60000); //60초 타임아웃
		
		try {
			if(!login(id, pw)){
				isLogin = false;
				System.out.println("로그인 실패!. 아이디와 패스워드를 확인하세요.");
			}
			else{
				System.out.println("로그인 성공!");
				isLogin = true;
			}
		} catch (Exception e) {}
	}
	
	/**
	 * 로그인 정보를 바탕으로 쿠키 생성
	 * @return 쿠키 정보가 담긴 Map
	 */
	private Map<String, String> makeLoginCookie(){
		cookies = new HashMap<>();
		
		CookieManager cookieManager = webClient.getCookieManager();
		Set<Cookie> cookieSet = cookieManager.getCookies();
		
		for(Cookie c : cookieSet){
			cookies.put(c.getName(), c.getValue());
		}
		return cookies;
	}
	
	/**
	 * 로그인 시도 메서드.
	 * @param naverId 입력한 네이버 아이디
	 * @param naverPw 입력한 네이버 비밀번호
	 * @return 성공시 true, 실패시 false
	 * @throws Exception webClient객체 예외처리
	 */
	private boolean login(String naverId, String naverPw) throws Exception {
		
		curPage = webClient.getPage(URL_LOGIN);
		
		HtmlForm form = curPage.getFormByName("frmNIDLogin");
		HtmlTextInput inputId = form.getInputByName("id");
		HtmlPasswordInput inputPw = (HtmlPasswordInput)form.getInputByName("pw");
		HtmlImageInput button = (HtmlImageInput)form.getByXPath("//input[@alt='로그인']").get(0);
		
		inputId.setValueAttribute(naverId);
		
		inputPw.setValueAttribute(naverPw);
		
		curPage = (HtmlPage)button.click();
		
		//Naver Sign in이 남아있으면 로그인 실패, 없으면 성공
		return !curPage.asText().contains("Naver Sign in");
	}
}