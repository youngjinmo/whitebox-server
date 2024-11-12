package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.TokenService;
import io.andy.shorten_url.auth.token.dto.CreateTokenDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;
import io.andy.shorten_url.exception.client.UnauthorizedException;

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
        CreateTokenDto createTokenDto = new CreateTokenDto(userId, userAgent, ipAddress);

        // when
        String token = tokenService.createToken(createTokenDto, 1000 * 60);

        // then
        assertNotNull(token);
        assertDoesNotThrow(() -> tokenService.verifyToken(new VerifyTokenDto(userId, userAgent, ipAddress, token))
        );
    }

    @Test
    @DisplayName("정상적으로 토큰 검증시 예외 미발생")
    void verifyToken() {
        // given
        CreateTokenDto createTokenDto= new CreateTokenDto(1L, "firefox", "127.0.0.1");
        String firstToken = tokenService.createToken(createTokenDto, ACCESS_TOKEN_EXPIRATION);
        VerifyTokenDto verifyTokenDto = new VerifyTokenDto(1L, "firefox", "127.0.0.1", firstToken);

        // when & then
        assertDoesNotThrow(() -> tokenService.verifyToken(verifyTokenDto));
    }

    @Test
    @DisplayName("다른 user id로 생성된 토큰 검증시도시 예외")
    void verifyTokenWithException() {
        // given
        CreateTokenDto createTokenDto= new CreateTokenDto(1L, "firefox", "127.0.0.1");
        String firstToken = tokenService.createToken(createTokenDto, ACCESS_TOKEN_EXPIRATION);
        VerifyTokenDto verifyTokenDto = new VerifyTokenDto(2L, "firefox", "127.0.0.1", firstToken);

        // when & then
        UnauthorizedException firstException = assertThrows(UnauthorizedException.class, () -> tokenService.verifyToken(verifyTokenDto));
        assertEquals("UNAUTHORIZED TOKEN", firstException.getMessage());
    }
}