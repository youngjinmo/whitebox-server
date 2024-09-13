package io.andy.shorten_url.util.random;

import org.springframework.stereotype.Component;

@Component
public interface RandomUtility {
    String generate(int length);
}
