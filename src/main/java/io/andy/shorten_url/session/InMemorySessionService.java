package io.andy.shorten_url.session;

import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class InMemorySessionService {
    public Object getSession(HttpServletRequest request, String key) {
        try {
            HttpSession session = request.getSession();

            return session.getAttribute(key);
        } catch (Exception e) {
            log.error("failed to get session, key={}, message={}", key, e.getMessage());
            throw new IllegalStateException();
        }
    }

    public void setSession(HttpServletRequest request, String key, Object value, int ttl) {
        try {
            HttpSession session = request.getSession();

            session.setAttribute(key, value);
            session.setMaxInactiveInterval(ttl);
        } catch (Exception e) {
            log.error("failed to set session, key={}, message={}", key, e.getMessage());
            throw new InternalServerException("FAILED TO SET SESSION");
        }
    }

    public boolean verifySession(HttpServletRequest request, String key, Object value) {
        try {
            HttpSession session = request.getSession();
            if (Objects.isNull(session) || Objects.isNull(session.getAttribute(key))) {
                return false;
            }
            return session.getAttribute(key).equals(value);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("failed to verify session, key={}, message={}", key, e.getMessage());
            throw new InternalServerException("FAILED TO VERIFY SESSION");
        }
    }

    public void invalidateSession(HttpServletRequest request, String key, Object value) {
        try {
            Object session = getSession(request, key);
            if (Objects.isNull(session) || !value.equals(session)) {
                log.debug("user={} failed to invalidate session={}", value, session);
                throw new UnauthorizedException("UNAUTHORIZED SESSION");
            }

            request.getSession().invalidate();
        } catch(UnauthorizedException e) {
           throw e;
        } catch (Exception e) {
            log.error("failed to remove session, error message={}", e.getMessage());
        }
    }
}
