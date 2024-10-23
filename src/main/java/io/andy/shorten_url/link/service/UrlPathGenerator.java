package io.andy.shorten_url.link.service;

import io.andy.shorten_url.util.random.Constants;
import io.andy.shorten_url.util.random.RandomUtility;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UrlPathGenerator implements RandomUtility {

    private final Random random = new Random();

    @Override
    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomInt = random.nextInt(Constants.ALL_CHARACTERS.length());
            sb.append(Constants.ALL_CHARACTERS.charAt(randomInt));
        }
        return String.valueOf(sb);
    }
}
