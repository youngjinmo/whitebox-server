package io.andy.shorten_url.auth;

import jakarta.mail.MessagingException;
import jakarta.validation.constraints.Min;

public interface AuthService {
    String sendEmailAuthCode(String recipient) throws MessagingException;
    String generateResetPassword(@Min(8) int length);
}
