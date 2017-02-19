package authentication;

import java.util.Map;

public interface ILogin {
	public Map<String, String> getCookies();
	
	public boolean isLogin();
}
