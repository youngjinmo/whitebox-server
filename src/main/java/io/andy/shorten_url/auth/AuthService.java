package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.auth.token.dto.TokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;

public interface AuthService {
    String encodePassword(String password);
    boolean matchPassword(String storedPassword, String givenPassword);
    String generateResetPassword();
    TokenResponseDto grantAuthToken(TokenRequestDto tokenRequestDto);
    VerifyTokenDto verifyAuthToken(String accessToken);
    void revokeAuthToken(String token);
    String createVerificationEmailKey(String recipient);
    String createVerificationResetPasswordKey(String recipient);
    String setEmailVerificationCode(String recipient, String sessionKey);
    boolean verifyResetPasswordCode(String username, String verificationCode);
    void verifyEmail(String recipient, String verificationCode);
}
