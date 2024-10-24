package io.andy.shorten_url.util.random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecretCodeGeneratorTest {

    private final SecretCodeGenerator codeGenerator = new SecretCodeGenerator();

    @Test
    @DisplayName("secret code 생성 검증")
    public void generate() {
        // given
        int length = 10;

        // when
        String generatedCode = codeGenerator.generate(length);

        // then
        assertNotNull(generatedCode);
        assertEquals(length, generatedCode.length());
    }
}