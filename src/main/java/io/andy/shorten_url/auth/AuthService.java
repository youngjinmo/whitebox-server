package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.auth.token.dto.CreateTokenDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;

import jakarta.validation.constraints.Min;

public interface AuthService {
    String encodePassword(String password);
    boolean matchPassword(String storedPassword, String givenPassword);
    String generateResetPassword(@Min(8) int length);
    TokenResponseDto grantAuthToken(CreateTokenDto createTokenDto);
    TokenResponseDto verifyAuthToken(VerifyTokenDto verifyTokenDto);
    void revokeAuthToken(Long userId, String token);
    void revokeAllSessionsByUserId(Long userId);
    String sendEmailVerificationCode(String recipient);
    void verifyEmail(String recipient, String verificationCode);
}
