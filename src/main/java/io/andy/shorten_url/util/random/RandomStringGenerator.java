package io.andy.shorten_url.util.random;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component("RandomStringGenerator")
public class RandomStringGenerator implements RandomUtility {

    private final String LowerCase = "abcdefghijklmnopqrstuvwxyz";
    private final String UpperCase = LowerCase.toUpperCase();
    private final String Digits = "0123456789";
    private final String AllCharacters = LowerCase.concat(UpperCase).concat(Digits);
    private final Random random = new Random();

    @Override
    public String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomInt = random.nextInt(AllCharacters.length());
            sb.append(AllCharacters.charAt(randomInt));
        }
        return String.valueOf(sb);
    }
}
