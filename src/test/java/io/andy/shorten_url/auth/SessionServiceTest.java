package io.andy.shorten_url.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private SessionService sessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("value 저장")
    void set() {
        // given
        String key = "session:key";
        String value = "value";
        long ttl = 1000L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        sessionService.set(key, value, ttl);

        // then
        verify(valueOperations, times(1)).set(key, value, ttl, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("value 가져오기 성공")
    void get() {
        // given
        String key = "session:key";
        String expectedValue = "value";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(expectedValue);

        // when
        Object actualValue = sessionService.get(key);

        // then
        assertEquals(expectedValue, actualValue);
    }

    @Test
    @DisplayName("키 삭제 성공")
    void delete() {
        // given
        String key = "session:key";

        // when
        sessionService.delete(key);

        // then
        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    @DisplayName("delete 메서드 - 잘못된 키로 IllegalArgumentException 발생")
    void deleteWithWrongArgument() {
        assertThrows(IllegalArgumentException.class, () -> sessionService.delete(null));
        assertThrows(IllegalArgumentException.class, () -> sessionService.delete(""));
    }

    @Test
    @DisplayName("와일드카드로 키 삭제 성공")
    void flushByWildcard() {
        // given
        String wildcardKey = "session:*";
        Set<String> keys = Set.of("session:1", "session:2");

        when(redisTemplate.keys(wildcardKey)).thenReturn(keys);

        // when
        sessionService.flushByWildcard(wildcardKey);

        // then
        verify(redisTemplate, times(keys.size())).delete(any(String.class));
    }

    @Test
    @DisplayName("flushByWildcard 메서드 - 빈 키 집합")
    void testFlushByWildcard_noKeys() {
        // given
        String wildcardKey = "session:*";

        when(redisTemplate.keys(wildcardKey)).thenReturn(Set.of());

        // when
        sessionService.flushByWildcard(wildcardKey);

        // then
        verify(redisTemplate, times(0)).delete(any(String.class));
    }
}