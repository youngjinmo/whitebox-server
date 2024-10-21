package io.andy.shorten_url.auth;

import io.andy.shorten_url.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(RedisConfig.class)
class SessionServiceTest {

    @Autowired private RedisService redisService;

    @Test
    void setSession() {
        String key = "at";
        String value = "529";
        redisService.setSession(key, value);

        assertNotNull(redisService.getSession(key));
        assertEquals(value, redisService.getSession(key));
    }
}