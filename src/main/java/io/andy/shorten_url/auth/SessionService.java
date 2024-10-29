package io.andy.shorten_url.auth;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class SessionService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, int ttlSeconds) {
        if (Objects.isNull(key) || key.isBlank() || Objects.isNull(value)) {
            throw new IllegalArgumentException("key or value cannot be null");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttl must be greater than 0");
        }
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.error("failed to set session, key={}, error message={}", key, e.getMessage());
            throw e;
        }
    }

    public Object get(String key) {
        if (Objects.isNull(key) || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be null");
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("failed to get session, key={}, error message={}", key, e.getMessage());
            throw e;
        }
    }

    public void delete(String key) {
        if (Objects.isNull(key) || key.isBlank()) {
            throw new IllegalArgumentException("key cannot be null");
        }
        try {
            redisTemplate.delete(key);
            log.info("session deleted, key={}", key);
        } catch (Exception e) {
            log.error("failed to delete session, key={}, error message={}", key, e.getMessage());
            throw e;
        }
    }

    public void clear() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (!Objects.isNull(keys) && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("session cleared, keys={}", keys);
            }
        } catch (Exception e) {
            log.error("failed to clear session, error message={}", e.getMessage());
        }
    }
}
