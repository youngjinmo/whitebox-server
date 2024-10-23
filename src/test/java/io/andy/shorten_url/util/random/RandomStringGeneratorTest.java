package io.andy.shorten_url.util.random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomStringGeneratorTest {
    private final RandomUtility randomUtil = new RandomStringGenerator();

    @RepeatedTest(10)
    @DisplayName("get random string")
    public void generate() {
        int length = 22;
        String randomString = randomUtil.generate(length);

        assertNotNull(randomString);
        assertEquals(length, randomString.length());
    }

    @Test
    @DisplayName("음수를 인자로 받을시 예외")
    public void throwIllegalArgumentException() {
        int length = -1;
        assertThrows(IllegalArgumentException.class, () -> randomUtil.generate(length));
    }
}
