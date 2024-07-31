package com.tenten.studybadge.common.constant;

public class MailConstant {

    public static final String SIGNUP_SUBJECT = "Study Badge 가입 인증 메일입니다.";

    public static final String RESET_PASSWORD_SUBJECT = "Study Badge 비밀번호 변경 인증 메일입니다.";

    public static final String UNICODE = "UTF-8";

    public static final String BASE_URL = "https://study-badge.vercel.app/SignUp/mailAuth";
    public static final String SIGNUP_BODY =  """
                                                <h3> 안녕하세요, Study Badge 가입을 환영합니다.
                                                아래 링크를 클릭해, 인증을 완료해주세요.</h3>
                                                <div><a href="%s?email=%s&code=%s">
                                                인증하기 </a></div>
                                              """;

    public static final String RESET_PASSWORD_BODY =  """
                                                <h3> 안녕하세요, Study Badge 비밀번호 변경 메일입니다.</h3>
                                                <p>아래 링크를 클릭해, 인증코드를 입력하여 인증을 완료해주시고, 비밀번호를 변경해주세요.</p>
                                                <p class="code">인증코드: %s</p>
                                                <div><a href="https://study-badge.vercel.app/resetPassword?email=%s">
                                                변경 링크 </a></div> 
                                              """;
}
