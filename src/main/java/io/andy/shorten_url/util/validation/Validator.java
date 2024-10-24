package io.andy.shorten_url.util.validation;

import java.util.Objects;

public class Validator {
    private Validator() {}

    public static boolean validateUrl(String url) {
       return Objects.nonNull(url)
               && url.matches("^(https?://)?([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})(:\\d+)?(/.*)?$");
    }

    public static boolean validateEmail(String email) {
        return Objects.nonNull(email)
                && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}
