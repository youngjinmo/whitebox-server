package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.TokenService;
import io.andy.shorten_url.auth.token.TokenType;
import io.andy.shorten_url.auth.token.dto.CreateAuthTokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyAuthTokenDto;
import io.andy.shorten_url.exception.client.UnauthorizedException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.context.ActiveProfiles;

import static io.andy.shorten_url.auth.AuthPolicy.ACCESS_TOKEN_EXPIRATION;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    private final TokenService tokenService = new TokenService();

    @Test
    @DisplayName("access token 생성 및 검증")
    void createAccessToken() {
        // given
        Long userId = 1L;
        String userAgent = "chrome";
        CreateAuthTokenRequestDto createAuthTokenRequestDto = new CreateAuthTokenRequestDto(userId, userAgent);

        // when
        String token = tokenService.createToken(TokenType.ACCESS_TOKEN, createAuthTokenRequestDto, ACCESS_TOKEN_EXPIRATION);

        // then
        assertNotNull(token);
        assertTrue(tokenService.verifyToken(TokenType.ACCESS_TOKEN, new VerifyAuthTokenDto(userId, userAgent, token)));
    }

    @Test
    @DisplayName("다른 user id로 생성된 토큰 검증시도시 예외")
    void verifyAccessTokenWithException() {
        // given
        CreateAuthTokenRequestDto firstTokenDto = new CreateAuthTokenRequestDto(1L, "firefox");
        String firstToken = createMockToken(firstTokenDto);

        CreateAuthTokenRequestDto secondTokenDto = new CreateAuthTokenRequestDto(2L, "chrome");
        String secondToken = createMockToken(secondTokenDto);

        // when
        VerifyAuthTokenDto verifyFirstTokenDto = new VerifyAuthTokenDto(2L, "firefox", firstToken);
        UnauthorizedException firstException = assertThrows(UnauthorizedException.class,
                () -> tokenService.verifyToken(TokenType.ACCESS_TOKEN, verifyFirstTokenDto));
        VerifyAuthTokenDto verifySecondTokenDto = new VerifyAuthTokenDto(1L, "chrome", secondToken);
        UnauthorizedException secondException = assertThrows(UnauthorizedException.class,
                () -> tokenService.verifyToken(TokenType.ACCESS_TOKEN, verifySecondTokenDto));

        // then
        assertEquals("UNAUTHORIZED TOKEN", firstException.getMessage());
        assertEquals("UNAUTHORIZED TOKEN", secondException.getMessage());
    }

    @Test
    @DisplayName("TokenType enum을 문자열로 변환")
    void convertTokenEnums() {
        assertEquals("access", tokenService.convertTokenEnums(TokenType.ACCESS_TOKEN));
        assertEquals("refresh", tokenService.convertTokenEnums(TokenType.REFRESH_TOKEN));
    }

    private String createMockToken(CreateAuthTokenRequestDto dto) {
        return tokenService.createToken(TokenType.ACCESS_TOKEN, dto, ACCESS_TOKEN_EXPIRATION);
    }
}