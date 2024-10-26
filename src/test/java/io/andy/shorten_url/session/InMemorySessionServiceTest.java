package io.andy.shorten_url.session;

import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemorySessionServiceTest {

    @Mock HttpServletRequest request;
    @Mock HttpSession session;
    @InjectMocks private InMemorySessionService sessionService;

    private static final String key = "test_key";
    private static final String value = "value";
    private static final int ttl = 100;

    @Test
    @DisplayName("세션 조회")
    void getSession() {
        // given
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(key)).thenReturn(value);

        // when
        Object result = sessionService.getSession(request, key);

        // then
        assertEquals(value, result);
    }

    @Test
    @DisplayName("세션이 존재하지 않을때 예외 발생")
    void getSession_exception() {
        // given
        when(request.getSession()).thenReturn(null);

        // then
        assertThrows(IllegalStateException.class,
                () -> sessionService.getSession(request, key));
    }

    @Test
    @DisplayName("세션 저장")
    void setSession() {
        // given
        when(request.getSession()).thenReturn(session);
        doNothing().when(session).setAttribute(key, value);

        // when
        sessionService.setSession(request, key, value, ttl);

        // then
        verify(session).setAttribute(key, value);
        verify(session).setMaxInactiveInterval(ttl);
    }

    @Test
    @DisplayName("세션 저장시 예외 발생했을때 500 반환")
    void setSession_exception() {
        // given
        when(request.getSession()).thenReturn(session);
        doThrow(new IllegalStateException()).when(session).setAttribute(key, ttl);

        // when
        InternalServerException exception = assertThrows(InternalServerException.class,
                () -> sessionService.setSession(request, key, value, ttl));

        // then
        assertEquals("FAILED TO SET SESSION", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"value, true", "other_value, false"})
    @DisplayName("세션 검증")
    void verifySession(String mockValue, boolean expect) {
        // given
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(key)).thenReturn(value);

        // when
        boolean result = sessionService.verifySession(request, key, mockValue);

        // then
        assertEquals(expect, result);
    }

    @Test
    @DisplayName("세션 비활성화")
    void invalidateSession() {
        // given
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(key)).thenReturn(value);

        // when
        sessionService.invalidateSession(request, key, value);

        // then
        verify(session, times(1)).invalidate();
    }

    @Test
    @DisplayName("세션 비활성화시 예외")
    void testInvalidateSession_Exception() {
        // given
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(key)).thenReturn(value);

        // when
        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> sessionService.invalidateSession(request, key, "other value"));

        // then
        assertEquals("UNAUTHORIZED SESSION", exception.getMessage());
    }
}