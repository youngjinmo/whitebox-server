package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.TokenService;
import io.andy.shorten_url.auth.token.TokenType;
import io.andy.shorten_url.auth.token.dto.CreateAuthTokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyAuthTokenDto;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.exception.server.TokenExpiredException;
import io.andy.shorten_url.util.mail.MailService;
import io.andy.shorten_url.util.mail.dto.MailMessageDto;
import io.andy.shorten_url.util.random.RandomUtility;

import io.jsonwebtoken.JwtException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.Min;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static io.andy.shorten_url.auth.AuthPolicy.*;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final MailService mailService;
    private final SessionService sessionService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RandomUtility randomUtility;

    public AuthServiceImpl(
            MailService mailService,
            SessionService sessionService,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            @Qualifier("SecretCodeGenerator") RandomUtility randomUtility
    ) {
        this.sessionService = sessionService;
        this.mailService = mailService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.randomUtility = randomUtility;
    }

    @Override
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean matchPassword(String storedPassword, String inputPassword) {
        return passwordEncoder.matches(storedPassword, inputPassword);
    }

    @Override
    public String sendEmailAuthCode(String recipient) throws MailException, MessagingException {
        try {
            String secretCode = randomUtility.generate(SECRET_CODE_LENGTH);
            String subject = "[Shorten-url] 이메일 인증";
            String body = "<h3>요청하신 인증 번호입니다.</h3><br>"+secretCode+"<br>";
            MailMessageDto messageDto = new MailMessageDto(recipient, subject, body);

            MimeMessage message = mailService.createMailMessage(messageDto);
            mailService.sendMail(recipient, message);

            return secretCode;
        } catch (MessagingException | MailException e) {
            log.error("failed to send email auth code, recipient = {}, error message = {}, stack trace = {}", recipient, e.getMessage(), e.getStackTrace());
            throw e;
        }
    }

    @Override
    public String generateResetPassword(@Min(8) int length) {
        return randomUtility.generate(10);
    }

    @Override
    public String createAccessTokenKey(Long userId, String userAgent) {
        return createTokenKey(TokenType.ACCESS_TOKEN, userId, userAgent);
    }

    @Override
    public String createRefreshTokenKey(Long userId, String userAgent) {
        return createTokenKey(TokenType.REFRESH_TOKEN, userId, userAgent);
    }

    @Override
    public String createWildcardKey(Long userId) {
        return String.format("auth:%s:*", userId);
    }

    @Override
    public String getTokenByKey(String tokenKey) {
        Object storedToken = sessionService.get(tokenKey);
        if (Objects.isNull(storedToken)) {
            throw new UnauthorizedException("NOT FOUND TOKEN IN THE SESSION STORAGE");
        }
        return String.valueOf(storedToken);
    }

    @Override
    public String grantAccessToken(CreateAuthTokenRequestDto tokenDto) {
        String tokenKey = createAccessTokenKey(tokenDto.userId(), tokenDto.userAgent());
        if (sessionService.get(tokenKey) != null) {
            sessionService.delete(tokenKey);
        }
        String accessToken = tokenService.createToken(TokenType.ACCESS_TOKEN, tokenDto, ACCESS_TOKEN_EXPIRATION);
        sessionService.set(tokenKey, accessToken, ACCESS_TOKEN_EXPIRATION);
        log.info("save access token into the session storage");
        return accessToken;
    }

    @Override
    public String grantRefreshToken(CreateAuthTokenRequestDto tokenDto) {
        String tokenKey = createRefreshTokenKey(tokenDto.userId(), tokenDto.userAgent());
        if (sessionService.get(tokenKey) != null) {
            sessionService.delete(tokenKey);
        }
        String refreshToken = tokenService.createToken(TokenType.REFRESH_TOKEN, tokenDto, REFRESH_TOKEN_EXPIRATION);
        sessionService.set(tokenKey, refreshToken, REFRESH_TOKEN_EXPIRATION);
        log.info("save access token into the session storage");
        return refreshToken;
    }

    @Override
    public String verifyAccessToken(VerifyAuthTokenDto tokenDto) {
        try {
            String accessTokenKey = createAccessTokenKey(tokenDto.userId(), tokenDto.userAgent());
            if (tokenService.verifyToken(TokenType.ACCESS_TOKEN, tokenDto)) {
                return getTokenByKey(accessTokenKey);
            }
        } catch (TokenExpiredException e) {
            try {
                String refreshTokenKey = createRefreshTokenKey(tokenDto.userId(), tokenDto.userAgent());
                String refreshToken = getTokenByKey(refreshTokenKey);

                // refresh token 검증
                VerifyAuthTokenDto refreshTokenDto = new VerifyAuthTokenDto(tokenDto.userId(), tokenDto.userAgent(), refreshToken);
                verifyRefreshToken(refreshTokenDto);
            } catch (TokenExpiredException | UnauthorizedException | JwtException ex) {
                throw new UnauthorizedException(ex.getMessage());
            }

            // access token, refresh token 갱신
            grantRefreshToken(new CreateAuthTokenRequestDto(tokenDto.userId(), tokenDto.userAgent()));
            String accessToken = grantAccessToken(new CreateAuthTokenRequestDto(tokenDto.userId(), tokenDto.userAgent()));
            log.info("refresh tokens by verify expired access token");
            return accessToken;
        }
        throw new InternalServerException();
    }

    @Override
    public String verifyRefreshToken(VerifyAuthTokenDto tokenDto) {
        String tokenKey = createRefreshTokenKey(tokenDto.userId(), tokenDto.userAgent());
        String refreshToken = getTokenByKey(tokenKey);
        tokenService.verifyToken(TokenType.REFRESH_TOKEN, tokenDto);
        return refreshToken;
    }

    @Override
    public void revokeAccessToken(VerifyAuthTokenDto tokenDto) {
        String key = createAccessTokenKey(tokenDto.userId(), tokenDto.userAgent());
        try {
            verifyAccessToken(tokenDto);
            sessionService.delete(key);
            log.info("revoked access token from the session storage, userId={}, userAgent={}", tokenDto.userId(), tokenDto.userAgent());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("failed to revoke access token, caused by={}", e.getMessage());
            throw new InternalServerException("FAILED TO REVOKE ACCESS TOKEN");
        }
    }

    @Override
    public void revokeRefreshToken(VerifyAuthTokenDto tokenDto) {
        String key = createRefreshTokenKey(tokenDto.userId(), tokenDto.userAgent());
        try {
            verifyRefreshToken(tokenDto);
            sessionService.delete(key);
            log.info("revoked refresh token from the session storage, userId={}, userAgent={}", tokenDto.userId(), tokenDto.userAgent());
        } catch (Exception e) {
            log.debug("failed to verify refresh token, caused by={}", e.getMessage());
        }
    }

    @Override
    public void revokeAllTokensByUserId(Long userId) {
        try {
            String wildcardKey = createWildcardKey(userId);
            sessionService.flushByWildcard(wildcardKey);
        } catch (Exception e) {
            log.error("failed to revoke tokens by userId={}, error message={}", userId, e.getMessage());
        }
    }

    private String createTokenKey(TokenType tokenType, Long userId, String userAgent) {
        return String.format("auth:%s:%s:%s", userId, userAgent, tokenService.convertTokenEnums(tokenType));
    }
}
