package io.andy.shorten_url.util.validation;

import java.util.Objects;

public class Validator {
    private Validator() {}

    public static boolean validateUrl(String url) {
        String URL_PATTERN =
                "^(https?://)?"                                // 프로토콜
                + "([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}"         // 도메인
                + "(:\\d{1,5})?"                            // 포트
                + "(/[a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=]*)?$"; // 경로
        return Objects.nonNull(url)
               && url.matches(URL_PATTERN);
    }

    public static boolean validateEmail(String email) {
        String EMAIL_PATTERN =
                "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@"  // 로컬 파트
                + "[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";   // 도메인 파트
        return Objects.nonNull(email)
                && email.matches(EMAIL_PATTERN);
    }
}
