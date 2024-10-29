package io.andy.shorten_url.auth;

import io.andy.shorten_url.config.RedisConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(RedisConfig.class)
class SessionServiceTest {

    @Autowired private SessionService sessionService;

    @AfterEach
    void clearAfter() {
        sessionService.clear();
    }

    @Test
    @DisplayName("get/set 검증")
    void setSession() {
        String key = "test:junit:1234";
        String value = "5678";
        sessionService.set(key, value, 100);

        assertNotNull(sessionService.get(key));
        assertEquals(value, sessionService.get(key));
    }
}