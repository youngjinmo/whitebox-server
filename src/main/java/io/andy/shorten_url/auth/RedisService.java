package io.andy.shorten_url.auth;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RedisService {
    private final RedisTemplate<String, AuthSession> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, AuthSession> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setSession(String key, String value) {
        AuthSession session = AuthSession.builder().prefix(key).value(value).build();
        redisTemplate.opsForValue().set(key, session);
    }

    public AuthSession getSession(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}
