package io.andy.shorten_url.auth;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class SessionService {
    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long ttl) {
        if (Objects.isNull(key) || key.isBlank() || Objects.isNull(value)) {
            throw new IllegalArgumentException("key or value cannot be null");
        }
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl must be greater than 0");
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.MILLISECONDS);
            log.info("save session, key={}, value={}, ttl={}", key, value, ttl);
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

    public void flushByWildcard(String wildCardKey) {
        try {
            Set<String> keys = redisTemplate.keys(wildCardKey);
            if (Objects.nonNull(keys) && !keys.isEmpty()) {
                for (String key : keys) {
                    redisTemplate.delete(key);
                }
                log.info("flushed session, keys={}", keys);
            }
            log.info("not found by wildcard key={}", wildCardKey);
        } catch (Exception e) {
            log.error("failed to clear session, error message={}", e.getMessage());
        }
    }
}
