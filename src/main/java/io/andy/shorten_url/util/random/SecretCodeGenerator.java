package io.andy.shorten_url.util.random;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component("SecretCodeGenerator")
public class SecretCodeGenerator implements RandomUtility {
    private final SecureRandom random = new SecureRandom();
    private final String CODE_CHARACTERS = Constants.UPPER_CASE.concat(Constants.DIGITS);

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
