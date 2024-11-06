package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.TokenService;
import io.andy.shorten_url.auth.token.TokenType;
import io.andy.shorten_url.auth.token.dto.CreateAuthTokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyAuthTokenDto;
import io.andy.shorten_url.config.RedisConfig;
import io.andy.shorten_url.exception.client.UnauthorizedException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Timer;
import java.util.TimerTask;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Import(RedisConfig.class)
class AuthServiceTest {
    @Autowired private TokenService tokenService;
    @Autowired private SessionService sessionService;
    @Autowired private AuthServiceImpl authService;

    @AfterEach
    void flushAfterEach() {
        sessionService.flushByWildcard("*");
    }

    @Test
    @DisplayName("패스워드 암호화")
    void encodePassword() {
        String rawPassword = "password";
        String encodedPassword = authService.encodePassword(rawPassword);
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("암호화 패스워드와 raw 패스워드 비교")
    void matchPassword() {
        // given
        String rawPassword = "password";
        String encodedPassword = authService.encodePassword(rawPassword);

        // when
        boolean result = authService.matchPassword(rawPassword, encodedPassword);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("token key 생성")
    void createTokenKey() {
        // given
        Long userId = 1L;
        String userAgent = "safari";

        // when
        String accessTokenKey = authService.createAccessTokenKey(userId, userAgent);
        String refreshTokenKey = authService.createRefreshTokenKey(userId, userAgent);
        String wildcardTokenKey = authService.createWildcardKey(userId);

        // then
        assertEquals("auth:1:safari:access", accessTokenKey);
        assertEquals("auth:1:safari:refresh", refreshTokenKey);
        assertEquals("auth:1:*", wildcardTokenKey);
    }

    @Test
    @DisplayName("token 조회")
    void getTokenByKey() {
        // given
        Long userId = 1L;
        String userAgent = "safari";
        String accessTokenKey = authService.createAccessTokenKey(userId, userAgent);
        String accessToken = authService.grantAccessToken(new CreateAuthTokenRequestDto(userId, userAgent));

        // when
        String storedToken = authService.getTokenByKey(accessTokenKey);

        // then
        assertEquals(accessToken, storedToken);
    }

    @Test
    @DisplayName("패스워드 초기화 위한 비밀번호 재발행")
    void generateResetPassword() {
        String resetPassword = authService.generateResetPassword(10);
        assertNotNull(resetPassword);
        assertEquals(10, resetPassword.length());
    }

    @Test
    @DisplayName("액세스 토큰 발행")
    void grantAccessToken() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";
        CreateAuthTokenRequestDto dto = new CreateAuthTokenRequestDto(userId, userAgent);
        String accessTokenKey = authService.createAccessTokenKey(userId, userAgent);

        // when
        String accessToken = authService.grantAccessToken(dto);
        String storedToken = authService.getTokenByKey(accessTokenKey);

        // then
        assertNotNull(accessToken);
        assertNotNull(storedToken);
        assertEquals(accessToken, storedToken);
    }

    @Test
    @DisplayName("액세스 토큰 검증")
    void verifyAccessToken() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";
        CreateAuthTokenRequestDto createTokenDto = new CreateAuthTokenRequestDto(userId, userAgent);
        String accessToken  = authService.grantAccessToken(createTokenDto);

        // 액세스 토큰과 다른 userId로 token 검증
        VerifyAuthTokenDto differentDto = new VerifyAuthTokenDto(2L, userAgent, accessToken);

        // when
        VerifyAuthTokenDto verifyTokenDto = new VerifyAuthTokenDto(userId, userAgent, accessToken);
        String verifiedAccessToken = authService.verifyAccessToken(verifyTokenDto);

        // then
        assertEquals(accessToken, verifiedAccessToken);
        assertThrows(UnauthorizedException.class,
                () -> authService.verifyAccessToken(differentDto));
    }

    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰 갱신")
    void refreshAccessTokenByRefreshToken() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";

        CreateAuthTokenRequestDto createTokenDto = new CreateAuthTokenRequestDto(userId, userAgent);
        String accessTokenKey = authService.createAccessTokenKey(userId, userAgent);
        String accessToken = tokenService.createToken(TokenType.ACCESS_TOKEN, createTokenDto, 1000);
        sessionService.set(accessTokenKey, accessToken, 1000);

        String refreshTokenKey = authService.createRefreshTokenKey(userId, userAgent);
        String refreshToken = tokenService.createToken(TokenType.REFRESH_TOKEN, createTokenDto, AuthPolicy.REFRESH_TOKEN_EXPIRATION);
        sessionService.set(refreshTokenKey, refreshToken, AuthPolicy.REFRESH_TOKEN_EXPIRATION);

        VerifyAuthTokenDto verifyAccessTokenDto = new VerifyAuthTokenDto(userId, userAgent, accessToken);

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // when
                String newAccessToken = authService.verifyAccessToken(verifyAccessTokenDto);
                String newRefreshToken = authService.getTokenByKey(refreshTokenKey);

                // then
                assertNotEquals(accessToken, newAccessToken);
                assertNotEquals(refreshToken, newRefreshToken);
            }
        };
        timer.schedule(task, 1000 * 2);
    }

    @Test
    @DisplayName("액세스 토큰 비활성화")
    void revokeAccessToken() {
        Long userId = 1L;
        String userAgent = "chromej";
        String accessToken  = authService.grantAccessToken(new CreateAuthTokenRequestDto(userId, userAgent));
        String tokenKey = authService.createAccessTokenKey(userId, userAgent);

        // revoke 전 redis에서 토큰 확인
        String storedToken = authService.getTokenByKey(tokenKey);
        assertNotNull(storedToken);
        assertEquals(accessToken, storedToken);

        // revoke token
        assertDoesNotThrow(() -> authService.revokeAccessToken(
                new VerifyAuthTokenDto(userId, userAgent, accessToken))
        );

        // revoke 이후 redis에서 토큰 조회
        assertNull(sessionService.get(tokenKey));
    }

    @Test
    @DisplayName("리프레시 토큰 발행")
    void grantRefreshToken() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";
        CreateAuthTokenRequestDto dto = new CreateAuthTokenRequestDto(userId, userAgent);
        String refreshTokenKey = authService.createRefreshTokenKey(userId, userAgent);

        // when
        String refreshToken = authService.grantRefreshToken(dto);

        // then
        assertNotNull(refreshToken);
        String storedToken = authService.getTokenByKey(refreshTokenKey);
        assertNotNull(storedToken);
        assertEquals(refreshToken, storedToken);
    }

    @Test
    @DisplayName("리프레시 토큰 검증")
    void verifyRefreshToken() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";
        String refreshToken = authService.grantRefreshToken(
                new CreateAuthTokenRequestDto(userId, userAgent));
        VerifyAuthTokenDto normalDto  = new VerifyAuthTokenDto(userId, userAgent, refreshToken);

        // 액세스 토큰과 다른 userId로 token 검증
        VerifyAuthTokenDto differentDto = new VerifyAuthTokenDto(2L, "safari", refreshToken);

        // when
        String verifiedRefreshToken = authService.verifyRefreshToken(normalDto);

        // then
        assertNotNull(verifiedRefreshToken);
        assertThrows(UnauthorizedException.class,
                () -> authService.verifyRefreshToken(differentDto));
    }

    @Test
    @DisplayName("리프레시 토큰 비활성화")
    void revokeRefreshToken() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";
        String refreshToken = authService.grantRefreshToken(new CreateAuthTokenRequestDto(userId, userAgent));
        String tokenKey = authService.createRefreshTokenKey(userId, userAgent);
        String storedToken = authService.getTokenByKey(tokenKey);

        // revoke 전 redis에서 토큰 확인
        assertNotNull(storedToken);
        assertEquals(refreshToken, storedToken);

        // revoke token
        VerifyAuthTokenDto tokenDto = new VerifyAuthTokenDto(userId, userAgent, refreshToken);
        assertDoesNotThrow(() -> authService.revokeRefreshToken(tokenDto));

        // revoke 이후 redis에서 토큰 조회
        assertNull(sessionService.get(tokenKey));
    }

    @Test
    @DisplayName("wildcard로 토큰 비활성화")
    void revokeAllTokensByUserId() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";
        String accessTokenKey = authService.createAccessTokenKey(userId, userAgent);
        String refreshTokenKey = authService.createRefreshTokenKey(userId, userAgent);
        String accessToken = authService.grantAccessToken(new CreateAuthTokenRequestDto(userId, userAgent));
        String refreshToken = authService.grantRefreshToken(new CreateAuthTokenRequestDto(userId, userAgent));

        // revoke 전 redis에서 토큰 확인
        String storedAccessToken = authService.getTokenByKey(accessTokenKey);
        String storedRefreshToken = authService.getTokenByKey(refreshTokenKey);

        assertNotNull(storedAccessToken);
        assertNotNull(storedRefreshToken);
        assertEquals(accessToken, storedAccessToken);
        assertEquals(refreshToken, storedRefreshToken);

        // when
        authService.revokeAllTokensByUserId(userId);

        // then
        assertNull(sessionService.get(accessTokenKey));
        assertNull(sessionService.get(refreshTokenKey));
    }
}