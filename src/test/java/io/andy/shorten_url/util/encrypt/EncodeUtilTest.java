package io.andy.shorten_url.util.encrypt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncodeUtilTest {

    @Test
    void encrypt() {
        String input = "hello";
        String encrypted = EncodeUtil.encrypt(input);

        assertNotNull(encrypted);
        assertNotEquals(input, encrypted);
    }

    @Test
    @DisplayName("인자가 비어있을때 예외반환")
    void IllegalArgumentException() {
        String input = "";
        assertThrows(IllegalArgumentException.class, () -> EncodeUtil.encrypt(input));
    }
}