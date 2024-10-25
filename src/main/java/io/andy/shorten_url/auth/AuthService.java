package io.andy.shorten_url.auth;

import jakarta.mail.MessagingException;

public interface AuthService {
    String sendEmailAuthCode(String recipient) throws MessagingException;
}
