package io.andy.shorten_url.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import static io.andy.shorten_url.session.SessionPolicy.SESSION_ACTIVE_TIME;

@Slf4j
@Service
public class SessionService {
    private final String LOGIN_SESSION_NAME = "LOGIN";

    public Object getSession(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession();

            return session.getAttribute(LOGIN_SESSION_NAME);
        } catch (Exception e) {
            log.error("failed to get session, request={}, error message={}",request, e.getMessage());
            throw new IllegalStateException("FAILED TO GET SESSION");
        }
    }

    public void setSessionById(HttpServletRequest request, Long id) {
        try {
            HttpSession session = request.getSession();

            session.setAttribute(LOGIN_SESSION_NAME, id);
            session.setMaxInactiveInterval(SESSION_ACTIVE_TIME);
        } catch (Exception e) {
            log.error("failed to set session, id={}, error message={}",id, e.getMessage());
            throw new IllegalStateException("FAILED TO SET SESSION");
        }
    }

    public void removeSessionById(HttpServletRequest request, Long id) {
        try {
            HttpSession session = request.getSession();

            session.invalidate();
        } catch (Exception e) {
            log.error("failed to remove session, id={}, error message={}",id, e.getMessage());
            throw new IllegalStateException("FAILED TO REMOVE SESSION");
        }
    }
}
