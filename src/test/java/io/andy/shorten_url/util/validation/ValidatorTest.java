package io.andy.shorten_url.util.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "https://github.com, true",
            "www.google.com, true",
            "naver.com, true",
            "localhost, false"
    })
    @DisplayName("URL 형식 검증")
    void invalidateUrl(String url, boolean expected) {
        boolean result = Validator.validateUrl(url);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
            "hello@gmail.com, true",
            "hello@, false"
    })
    @DisplayName("이메일 형식 검증")
    void invalidateEmail(String email, boolean expected) {
        boolean result = Validator.validateEmail(email);
        assertEquals(expected, result);
    }
}