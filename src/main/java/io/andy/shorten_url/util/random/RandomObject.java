package io.andy.shorten_url.util.random;

import org.springframework.stereotype.Component;

@Component
public interface RandomObject<T> {
    T generate(int length);
}
