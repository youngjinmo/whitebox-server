package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.TokenService;
import io.andy.shorten_url.auth.token.dto.CreateTokenDto;
import io.andy.shorten_url.auth.token.dto.TokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;
import io.andy.shorten_url.exception.server.TokenExpiredException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.andy.shorten_url.auth.AuthPolicy.ACCESS_TOKEN_EXPIRATION;
import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {
    private final TokenService tokenService = new TokenService();

    @Test
    @DisplayName("access token 생성 및 검증")
    void createToken() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";
        String ipAddress = "127.0.0.1";
        TokenRequestDto tokenRequestDto = TokenRequestDto.build(userId, userAgent, ipAddress);

        // when
        String token = tokenService.createToken(CreateTokenDto.of(tokenRequestDto, 1000 * 60));

        // then
        assertNotNull(token);
        assertDoesNotThrow(() -> tokenService.verifyToken(token));
    }

    @Test
    @DisplayName("정상적으로 토큰 검증시 예외 미발생")
    void verifyToken() {
        // given
        Long userId = 1L;
        String ipAddress = "127.0.0.1";
        String userAgent = "Firefox";

        TokenRequestDto tokenRequestDto = TokenRequestDto.build(1L, ipAddress, userAgent);
        String firstToken = tokenService.createToken(CreateTokenDto.of(tokenRequestDto, ACCESS_TOKEN_EXPIRATION));
        VerifyTokenDto verifyTokenDto = VerifyTokenDto.build(1L, ipAddress, userAgent, firstToken);

        // when
        VerifyTokenDto tokenResponse = tokenService.verifyToken(verifyTokenDto.getToken());

        // then
        assertEquals(tokenResponse.getUserId(), 1L);
        assertEquals(tokenResponse.getIpAddress(), ipAddress);
        assertEquals(tokenResponse.getUserAgent(), userAgent);
    }

    @Test
    @DisplayName("다른 user id로 생성된 토큰 검증시도시 예외")
    void verifyTokenWithException() throws InterruptedException {
        // given
        TokenRequestDto tokenRequestDto = TokenRequestDto.build(1L, "firefox", "127.0.0.1");
        String accessToken = tokenService.createToken(CreateTokenDto.of(tokenRequestDto, 1000)); // for a second

        // when & then
        Thread.sleep(1000 * 2); // after 2 seconds
        assertThrows(TokenExpiredException.class, () -> tokenService.verifyToken(accessToken));
    }
}