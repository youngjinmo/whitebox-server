package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.TokenService;
import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.auth.token.dto.CreateTokenDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.exception.server.TokenExpiredException;
import io.andy.shorten_url.util.random.RandomUtility;

import jakarta.validation.constraints.Min;

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
    public String generateResetPassword(@Min(8) int length) {
        return randomUtility.generate(length);
    }

    @Override
    public TokenResponseDto grantAuthToken(CreateTokenDto createTokenDto) {
        try {
            String accessToken = createAccessToken(createTokenDto);
            String refreshToken = createRefreshToken(createTokenDto);
            String tokenKey = createTokenKey(createTokenDto.userId(), accessToken);

            sessionService.set(tokenKey, refreshToken, REFRESH_TOKEN_EXPIRATION);
            return new TokenResponseDto(accessToken, refreshToken);
        } catch (Exception e) {
            log.error("failed to grant auth token, userId={}, ", createTokenDto.userId(), e);
            throw new InternalServerException("FAILED TO GRANT AUTH TOKEN");
        }
    }

    @Override
    @Transactional
    public TokenResponseDto verifyAuthToken(VerifyTokenDto verifyTokenDto) {
        // 토큰 키 생성
        String tokenKey = createTokenKey(verifyTokenDto.userId(), verifyTokenDto.token());

        try {
            // 액세스 토큰 검증
            tokenService.verifyToken(verifyTokenDto);

            // redis 에서 조회 (서버에서 발급된 토큰인지 검증)
            String refreshToken = getAuthTokenByKey(tokenKey);
            if (Objects.isNull(refreshToken)) {
                log.debug("not found access token in the session storage, userId={}", verifyTokenDto.userId());
                throw new UnauthorizedException();
            }

            // 인증 성공
            return new TokenResponseDto(verifyTokenDto.token(), refreshToken);

        } catch (TokenExpiredException e) {

            /*
                요청에 담긴 토큰 만료시, 리프레시 토큰 검증
                valid   -> redis에 토큰 있으면 토큰 갱신을 위해 세션 삭제
                invalid -> redis에 없으면 비정상적 토큰이기에 401 예외
             */
            try {
                String refreshToken = getAuthTokenByKey(tokenKey);
                revokeAuthToken(verifyTokenDto.userId(), refreshToken);
            } catch (NullPointerException | UnauthorizedException ex) {
                throw new UnauthorizedException("EXPIRED REFRESH TOKEN");
            }

            // 새 토큰 생성
            CreateTokenDto createTokenDto = new CreateTokenDto(verifyTokenDto);
            String accessToken = createAccessToken(createTokenDto);
            String refreshToken = createRefreshToken(createTokenDto);

            // redis 에 새 토큰 추가
            String newTokenKey = createTokenKey(verifyTokenDto.userId(), accessToken);
            sessionService.set(newTokenKey, refreshToken, REFRESH_TOKEN_EXPIRATION);

            return new TokenResponseDto(accessToken, refreshToken);
        }
    }

    @Override
    public void revokeAuthToken(Long userId, String token) {
        String tokenKey = createTokenKey(userId, token);

        String storedAuthToken = getAuthTokenByKey(tokenKey);
        if (Objects.isNull(storedAuthToken)) {
            log.debug("not found auth token in the session storage, userId={}", userId);
            throw new UnauthorizedException();
        }

        try {
            sessionService.delete(tokenKey);
        } catch (Exception e) {
            log.error("failed to revoke auth token, userId={}", userId);
            throw new InternalServerException();
        }
    }

    /**
     *  only use when delete account, do not use when logout
     * @param userId
     */
    @Override
    public void revokeAllSessionsByUserId(Long userId) {
        try {
            String wildcardKey = createWildcardKey(userId);
            sessionService.flushByWildcard(wildcardKey);
        } catch (Exception e) {
            log.error("failed to revoke tokens by userId={}, error message={}", userId, e.getMessage());
        }
    }

    @Override
    public String sendEmailVerificationCode(String recipient) {
        // create session key
        String key = createVerificationEmailKey(recipient);

        // check already sent verification code
        try {
            Object sent = sessionService.get(key);
            if (sent != null) {
                throw new BadRequestException("ALREADY SENT EMAIL VERIFICATION CODE");
            }
        } catch (BadRequestException e) {
            throw e;
        }catch (Exception e) {
            log.warn("failed to check already sent email auth verification code, recipient = {}, error message={}", recipient, e.getMessage());
        }

        // generate verification code
        String verificationCode = randomUtility.generate(SECRET_CODE_LENGTH);

        try {
            // save session in to the session storage
            sessionService.set(key, verificationCode, EMAIL_AUTH_SESSION_ACTIVE_TIME);
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

    private String createAccessToken(CreateTokenDto createTokenDto) {
        return tokenService.createToken(createTokenDto, ACCESS_TOKEN_EXPIRATION);
    }

    private String createRefreshToken(CreateTokenDto createTokenDto) {
        return tokenService.createToken(createTokenDto, REFRESH_TOKEN_EXPIRATION);
    }

    private String getAuthTokenByKey(String key) {
        Object refreshToken = sessionService.get(key);
        if (Objects.isNull(refreshToken)) {
            throw new UnauthorizedException("NOT FOUND TOKEN IN THE SESSION STORAGE");
        }
        return String.valueOf(refreshToken);
    }

    private String getVerificationEmailCodeByKey(String key) {
        Object verificationCode = sessionService.get(key);
        if (Objects.isNull(verificationCode)) {
            throw new UnauthorizedException("NOT FOUND VERIFICATION CODE IN THE SESSION STORAGE");
        }
        return String.valueOf(verificationCode);
    }

    private String createTokenKey(Long userId, String accessToken) {
        return String.format("%s:%s:%s", AUTH_TOKEN_KEY_PREFIX, userId, accessToken);
    }

    private String createWildcardKey(Long userId) {
        return String.format("%s:%s:*", AUTH_TOKEN_KEY_PREFIX, userId);
    }

    private String createVerificationEmailKey(String recipient) {
        return String.format("%s:%s", EMAIL_AUTH_SESSION_KEY_PREFIX, recipient);
    }
}
