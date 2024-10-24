package io.andy.shorten_url.util.random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SecretCodeGeneratorTest {

    private final SecretCodeGenerator codeGenerator = new SecretCodeGenerator();

    @Test
    @DisplayName("Generate code of specified length")
    public void testGenerateCodeLength() {
        // given
        int length = 10;

        // when
        String generatedCode = codeGenerator.generate(length);

        // then
        assertNotNull(generatedCode);
        assertEquals(length, generatedCode.length());
    }

    @Test
    @DisplayName("Generated code contains only valid characters")
    public void testGenerateCodeContainsValidCharacters() {
        // given
        int length = 15;
        String validCharacters = Constants.UPPER_CASE + Constants.DIGITS;

        // when
        String generatedCode = codeGenerator.generate(length);

        // then
        for (char c : generatedCode.toCharArray()) {
            assertTrue(validCharacters.indexOf(c) >= 0, "Invalid character found: " + c);
        }
    }

    @Test
    @DisplayName("Generated codes are unique")
    public void testGenerateUniqueCodes() {
        // given
        int length = 8;
        Set<String> generatedCodes = new HashSet<>();

        // when
        for (int i = 0; i < 1000; i++) {
            String newCode = codeGenerator.generate(length);
            assertFalse(generatedCodes.contains(newCode), "Duplicate code generated: " + newCode);
            generatedCodes.add(newCode);
        }

        // then
        assertEquals(1000, generatedCodes.size());
    }
}