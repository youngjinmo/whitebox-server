package io.andy.shorten_url.util.random;

import java.util.Random;

public class SecretCodeGenerator implements RandomUtility {
    private final Random random;
    private final String CODE_CHARACTERS;

    public SecretCodeGenerator() {
        CODE_CHARACTERS = Constants.UPPER_CASE.concat(Constants.DIGITS);
        random = new Random();
    }

    @Override
    public String generate(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomInt = random.nextInt(CODE_CHARACTERS.length());
            sb.append(CODE_CHARACTERS.charAt(randomInt));
        }
        return String.valueOf(sb);
    }
}
