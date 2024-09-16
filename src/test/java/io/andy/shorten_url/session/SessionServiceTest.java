package io.andy.shorten_url.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static io.andy.shorten_url.session.SessionPolicy.SESSION_KEY_LOGIN;

import static org.junit.jupiter.api.Assertions.*;

class SessionServiceTest {

    private MockHttpServletRequest request;
    private SessionService sessionService;

    @BeforeEach
    public void setMockRequest() {
        request = new MockHttpServletRequest();
        sessionService = new SessionService();
    }

    @Test
    @DisplayName("세선 저장")
    void setAttribute() {
        Long userId = 999L;

        sessionService.setAttribute(request, userId);

        Object session = request.getSession().getAttribute(SESSION_KEY_LOGIN);
        assertNotNull(session);
        assertEquals(userId, session);
    }

    @Test
    @DisplayName("세션에서 id 조회")
    void getAttribute() {
        Long userId = 999L;
        request.getSession().setAttribute(SESSION_KEY_LOGIN, userId);

        Object session = sessionService.getAttribute(request);
        assertNotNull(session);
        assertEquals(userId, session);
    }

    @Test
    @DisplayName("세션 무효화")
    void removeSession() {
        Long userId = 999L;
        request.getSession().setAttribute(SESSION_KEY_LOGIN, userId);
        assertNotNull(request.getSession().getAttribute(SESSION_KEY_LOGIN)); // 세션 저장 검증

        sessionService.invalidateSession(request, userId);

        assertNull(request.getSession().getAttribute(SESSION_KEY_LOGIN)); // 세션 무효화 검증
    }
}