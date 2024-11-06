package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.dto.CreateAuthTokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyAuthTokenDto;

import jakarta.mail.MessagingException;
import jakarta.validation.constraints.Min;

public interface AuthService {
    String encodePassword(String password);
    boolean matchPassword(String storedPassword, String givenPassword);
    String sendEmailAuthCode(String recipient) throws MessagingException;
    String generateResetPassword(@Min(8) int length);
    String createAccessTokenKey(Long userId, String userAgent);
    String createRefreshTokenKey(Long userId, String userAgent);
    String getTokenByKey(String tokenKey);
    String grantAccessToken(CreateAuthTokenRequestDto tokenRequestDto);
    String grantRefreshToken(CreateAuthTokenRequestDto tokenRequestDto);
    String verifyAccessToken(VerifyAuthTokenDto verifyTokenDto);
    String verifyRefreshToken(VerifyAuthTokenDto verifyTokenDto);
    void revokeAccessToken(VerifyAuthTokenDto verifyTokenDto);
    void revokeRefreshToken(VerifyAuthTokenDto verifyTokenDto);
    void revokeAllTokensByUserId(Long userId);
    String createWildcardKey(Long userId);
}
