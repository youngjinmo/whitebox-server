package io.andy.shorten_url.util.random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

class RandomNumberGeneratorTest {
    private final RandomUtility randomUtil = new RandomNumberGenerator();

    @RepeatedTest(10)
    @DisplayName("get random number")
    public void generate() {
        int length = 10;
        String randomNumber = randomUtil.generate(length);

        assertNotNull(randomNumber);
        assertEquals(length, randomNumber.length());
        assertTrue(randomNumber.matches("[0-9]+"));
    }
}
