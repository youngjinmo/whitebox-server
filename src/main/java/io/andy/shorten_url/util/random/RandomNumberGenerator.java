package io.andy.shorten_url.util.random;

import java.util.Random;

public class RandomNumberGenerator implements RandomUtility {

    Random random = new Random();

    @Override
    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomInt = random.nextInt(Constants.DIGITS.length());
            sb.append(Constants.DIGITS.charAt(randomInt));
        }
        return String.valueOf(sb);
    }
}
