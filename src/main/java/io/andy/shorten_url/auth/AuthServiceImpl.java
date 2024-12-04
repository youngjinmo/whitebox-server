package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.TokenService;
import io.andy.shorten_url.auth.token.dto.CreateTokenDto;
import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.auth.token.dto.TokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;
import io.andy.shorten_url.exception.client.ForbiddenException;
import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.exception.server.TokenExpiredException;
import io.andy.shorten_url.util.random.RandomUtility;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static io.andy.shorten_url.auth.AuthPolicy.*;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final SessionService sessionService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RandomUtility randomUtility;

    public AuthServiceImpl(
            SessionService sessionService,
            TokenService tokenService,
            PasswordEncoder passwordEncoder,
            @Qualifier("SecretCodeGenerator") RandomUtility randomUtility
    ) {
        this.sessionService = sessionService;
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
    public String generateResetPassword() {
        return randomUtility.generate(RESET_PASSWORD_LENGTH);
    }

    @Override
    public TokenResponseDto grantAuthToken(TokenRequestDto tokenRequestDto) {
        try {
            // create session key (prefix:access_token)
            String accessToken = createAccessToken(tokenRequestDto);
            String sessionKey = createSessionKey(accessToken);

            // create session value (userId:refresh_token)
            String refreshToken = createRefreshToken(tokenRequestDto);
            String sessionValue = createSessionValue(tokenRequestDto.getUserId(), refreshToken);

            sessionService.set(sessionKey, sessionValue, REFRESH_TOKEN_EXPIRATION);

            return TokenResponseDto.build(accessToken, refreshToken);
        } catch (Exception e) {
            log.error("failed to grant auth token, userId={}, error message={}", tokenRequestDto.getUserId(), e.getMessage());
            throw new InternalServerException("FAILED TO GRANT AUTH TOKEN");
        }
    }

    @Override
    @Transactional
    public VerifyTokenDto verifyAuthToken(String accessToken) {
        // 토큰 키 생성
        String tokenKey = createSessionKey(accessToken);

        try {
            // verify access token
            VerifyTokenDto verifyTokenDto = tokenService.verifyToken(accessToken);

            // get token from session storage(redis)
            if (Objects.isNull(getAuthTokenByKey(tokenKey))) {
                log.debug("not found access token in the session storage, userId={}", verifyTokenDto.getUserId());
                throw new UnauthorizedException();
            }

            // success to verified
            return verifyTokenDto;

        } catch (TokenExpiredException e) {

            /*
                요청에 담긴 토큰 만료시, 리프레시 토큰 검증
                valid   -> redis에 토큰 있으면 토큰 갱신을 위해 세션 삭제
                invalid -> redis에 없으면 비정상적 토큰이기에 401 예외
             */
            VerifyTokenDto verifyTokenDto = tokenService.verifyToken(accessToken);
            try {
                String refreshToken = getAuthTokenByKey(tokenKey);
                revokeAuthToken(refreshToken);
            } catch (UnauthorizedException ex) {
                throw new UnauthorizedException("EXPIRED REFRESH TOKEN");
            }

            // refresh tokens (access/refresh)
            TokenRequestDto tokenRequestDto = TokenRequestDto.from(verifyTokenDto);
            String newAccessToken = createAccessToken(tokenRequestDto);
            String refreshToken = createRefreshToken(tokenRequestDto);

            // set new token into the session storage
            String newTokenKey = createSessionKey(newAccessToken);
            sessionService.set(newTokenKey, refreshToken, REFRESH_TOKEN_EXPIRATION);

            return VerifyTokenDto.build(
                    verifyTokenDto.getUserId(),
                    verifyTokenDto.getIpAddress(),
                    verifyTokenDto.getUserAgent(),
                    refreshToken
            );
        }
    }

    @Override
    public void revokeAuthToken(String token) {
        String tokenKey = createSessionKey(token);

        String storedAuthToken = getAuthTokenByKey(tokenKey);
        if (Objects.isNull(storedAuthToken)) {
            log.info("not found auth token in the session storage");
            throw new NotFoundException();
        }

        try {
            sessionService.delete(tokenKey);
            log.info("revoked token");
        } catch (Exception e) {
            log.error("failed to revoke auth token");
            throw new InternalServerException();
        }
    }

    @Override
    public String createVerificationEmailKey(String recipient) {
        return String.format("%s:%s", EMAIL_AUTH_SESSION_KEY_PREFIX, recipient);
    }

    @Override
    public String createVerificationResetPasswordKey(String recipient) {
        return String.format("%s:%s", RESET_PASSWORD_SESSION_KEY_PREFIX, recipient);
    }

    @Override
    @Transactional
    public String setEmailVerificationCode(String recipient, String sessionKey) {

        // check already sent verification code
        try {
            Object sent = sessionService.get(sessionKey);
            if (sent != null) {
                throw new ForbiddenException("ALREADY SENT EMAIL VERIFICATION CODE");
            }
        } catch (ForbiddenException e) {
            throw e;
        }catch (Exception e) {
            log.warn("failed to check already sent email auth verification code, recipient = {}, error message={}", recipient, e.getMessage());
        }

        // generate verification code
        String verificationCode = randomUtility.generate(SECRET_CODE_LENGTH);

        try {
            // save session in to the session storage
            sessionService.set(sessionKey, verificationCode, EMAIL_AUTH_SESSION_ACTIVE_TIME);
            log.debug("save email verification code into the session storage");
        } catch (Exception e) {
            log.error("failed to send email verification code, userId={}", recipient, e);
            throw new InternalServerException("FAILED TO SEND EMAIL VERIFICATION CODE");
        }

        return verificationCode;
    }

    @Override
    public void verifyEmail(String recipient, String verificationCode) {
        // generate redis key
        String key = createVerificationEmailKey(recipient);

        // get redis value by key
        String code = getVerificationEmailCodeByKey(key);

        // verify email
        if (!verificationCode.equals(code)) {
            throw new UnauthorizedException("INVALID EMAIL BY VERIFIED");
        }
        sessionService.delete(key);
        log.info("verified email {}", recipient);
    }

    @Override
    public boolean verifyResetPasswordCode(String username, String verificationCode) {
        String sessionKey = createVerificationResetPasswordKey(username);
        Object code = sessionService.get(sessionKey);
        if (code instanceof String && ((String) code).equals(verificationCode)) {
            return true;
        }
        return false;
    }

    private String createAccessToken(TokenRequestDto tokenRequestDto) {
        return tokenService.createToken(CreateTokenDto.of(tokenRequestDto, ACCESS_TOKEN_EXPIRATION));
    }

    private String createRefreshToken(TokenRequestDto tokenRequestDto) {
        return tokenService.createToken(CreateTokenDto.of(tokenRequestDto, REFRESH_TOKEN_EXPIRATION));
    }

    private String getAuthTokenByKey(String key) {
        Object sessionValue = sessionService.get(key);
        if (Objects.isNull(sessionValue)) {
            throw new UnauthorizedException("NOT FOUND TOKEN IN THE SESSION STORAGE");
        }
        // return refresh token
        return String.valueOf(sessionValue).split(":")[1];
    }

    private String getVerificationEmailCodeByKey(String key) {
        Object verificationCode = sessionService.get(key);
        if (Objects.isNull(verificationCode)) {
            throw new UnauthorizedException("NOT FOUND VERIFICATION CODE IN THE SESSION STORAGE");
        }
        return String.valueOf(verificationCode);
    }

    private String createSessionKey(String accessToken) {
        return String.format("%s:%s", AUTH_TOKEN_KEY_PREFIX, accessToken);
    }

    private String createSessionValue(Long userId, String refreshToken) {
        return String.format("%d:%s", userId, refreshToken);
    }
}
