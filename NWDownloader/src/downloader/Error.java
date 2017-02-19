package downloader;

public class Error {
	/**
	 * 에러코드에 따른 에러메세지 출력: package access, static way
	 * @param errorCode 에러코드분류
	 * 1: 잘못된 주소 입력
	 * 2: 여러 만화 다운로드시 서로 다른 만화 주소 입력
	 * 3: 존재하지 않는 만화 주소 입력
	 * default: 알 수 없는 에러
	 */
	static void printErrMsg(int errorCode) {
		StringBuilder errMsg = new StringBuilder().append("다운로드 실패: ");
		switch (errorCode) {
		case 1:
			errMsg.append("잘못된 주소입니다.");
			break;
		case 2:
			errMsg.append("같은 만화의 주소를 입력해 주세요.");
			break;
		case 3:
			errMsg.append("없는 만화입니다.");
			break;
		case 4:
			errMsg.append("로그인 후 시도하세요.");
			break;
		default:
			errMsg.append("알수 없는 에러.");
		}
		System.out.println(errMsg);
	}
}
