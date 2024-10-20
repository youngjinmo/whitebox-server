package io.andy.shorten_url.session;

import io.andy.shorten_url.exception.client.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import static io.andy.shorten_url.session.SessionPolicy.SESSION_KEY_LOGIN;
import static io.andy.shorten_url.session.SessionPolicy.SESSION_ACTIVE_TIME;

@Slf4j
@Service
public class SessionService {

    public Object getAttribute(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession();

            return session.getAttribute(SESSION_KEY_LOGIN);
        } catch (Exception e) {
            log.error("failed to get session, request={}, error message={}",request, e.getMessage());
            throw new IllegalStateException("FAILED TO GET SESSION");
        }
    }

    public void setAttribute(HttpServletRequest request, Long userId) {
        try {
            HttpSession session = request.getSession();

            session.setAttribute(SESSION_KEY_LOGIN, userId);
            session.setMaxInactiveInterval(SESSION_ACTIVE_TIME);
        } catch (Exception e) {
            log.error("failed to set session, id={}, error message={}", userId, e.getMessage());
            throw new IllegalStateException("FAILED TO SET SESSION");
        }
    }

    public void invalidateSession(HttpServletRequest request, Long userId) {
        try {
            Object session = getAttribute(request);
            if (!userId.equals(session)) {
                log.debug("user={} failed to invalidate session={}", userId, session);
                throw new UnauthorizedException("UNAUTHORIZED SESSION");
            }

            request.getSession().invalidate();
        } catch (Exception e) {
            log.error("failed to remove session, error message={}", e.getMessage());
            throw new IllegalStateException("FAILED TO REMOVE SESSION");
        }
    }
}
