package io.andy.shorten_url.auth;

import io.andy.shorten_url.auth.token.TokenService;
import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.auth.token.dto.CreateTokenDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.TokenExpiredException;
import io.andy.shorten_url.util.random.RandomUtility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static io.andy.shorten_url.auth.AuthPolicy.SECRET_CODE_LENGTH;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private SessionService sessionService;
    @Mock private TokenService tokenService;
    @Mock private RandomUtility randomUtility;
    @Spy private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @InjectMocks private AuthServiceImpl authService;

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
    @DisplayName("Auth 토큰 발행")
    void grantAuthToken() {
        // given
        CreateTokenDto tokenDto = new CreateTokenDto(1L, "chrome", "127.0.0.1");
        String mockAccessToken = "mock-access-token";
        String mockRefreshToken = "mock-refresh-token";

        when(tokenService.createToken(any(CreateTokenDto.class), anyLong()))
                .thenReturn(mockAccessToken)
                .thenReturn(mockRefreshToken);
        doNothing().when(sessionService).set(anyString(), anyString(), anyLong());

        // when
        TokenResponseDto tokenResponseDto = authService.grantAuthToken(tokenDto);

        // then
        assertEquals(mockAccessToken, tokenResponseDto.accessToken());
        assertEquals(mockRefreshToken, tokenResponseDto.refreshToken());
        verify(sessionService, times(1)).set(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("정상 토큰으로 검증시 성공")
    void verifyAuthToken() {
        // given
        String mockAccessToken = "mock-access-token";
        String mockRefreshToken = "mock-refresh-token";
        CreateTokenDto createTokenDto = new CreateTokenDto(1L, "chrome", "127.0.0.1");

        doNothing().when(tokenService).verifyToken(any(VerifyTokenDto.class));
        when(sessionService.get(anyString())).thenReturn(mockRefreshToken);

        // when
        TokenResponseDto result = authService.verifyAuthToken(new VerifyTokenDto(createTokenDto, mockAccessToken));

        // then
        assertEquals(mockAccessToken, result.accessToken());
        assertEquals(mockRefreshToken, result.refreshToken());
        verify(sessionService, times(0)).set(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("만료된 액세스 토큰으로 검증시 리프레싱")
    void refreshTokenByExpiredToken() {
        // given
        String mockAccessToken = "mock-access-token";
        String mockRefreshToken = "mock-refresh-token";
        CreateTokenDto createTokenDto = new CreateTokenDto(1L, "chrome", "127.0.0.1");

        doThrow(TokenExpiredException.class).when(tokenService).verifyToken(any(VerifyTokenDto.class));
        when(sessionService.get(anyString())).thenReturn(mockRefreshToken);
        doNothing().when(sessionService).delete(anyString());
        when(tokenService.createToken(any(CreateTokenDto.class), anyLong()))
                .thenReturn("new-access-token")
                .thenReturn("new-refresh-token");

        // when
        TokenResponseDto result = authService.verifyAuthToken(new VerifyTokenDto(createTokenDto, mockAccessToken));

        // then
        assertNotEquals(mockAccessToken, result.accessToken());
        assertNotEquals(mockRefreshToken, result.refreshToken());
        verify(sessionService, times(1)).delete(anyString());
        verify(sessionService, times(1)).set(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("모든 토큰이 만료되어 401 예외 처리")
    void refreshAllTokensWithExpiredRefreshToken() {
        // given
        String mockAccessToken = "mock-access-token";
        CreateTokenDto createTokenDto = new CreateTokenDto(1L, "chrome", "127.0.0.1");

        doThrow(TokenExpiredException.class).when(tokenService).verifyToken(any(VerifyTokenDto.class));
        when(sessionService.get(anyString())).thenReturn(null);

        // when & then
        assertThrows(UnauthorizedException.class, () -> authService.verifyAuthToken(new VerifyTokenDto(createTokenDto, mockAccessToken)));
    }

    @Test
    @DisplayName("토큰 비활성화")
    void revokeAuthToken() {
        // given
        Long userId = 1L;
        String mockAccessToken = "mock-access-token";

        when(sessionService.get(anyString())).thenReturn(mockAccessToken);

        // when & then
        assertDoesNotThrow(() -> authService.revokeAuthToken(userId, mockAccessToken));
        verify(sessionService, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("userId 기반으로 모든 세션 삭제 (탈퇴)")
    void revokeAllSessionsByUserId() {
        // given
        Long userId = 1L;

        doNothing().when(sessionService).flushByWildcard(anyString());

        // when
        assertDoesNotThrow(() -> authService.revokeAllSessionsByUserId(userId));
    }

    @Test
    @DisplayName("이메일 인증 코드 발송")
    void sendEmailVerificationCode() {
        // given
        String mockVerificationCode = "mock-verification-code";

        when(sessionService.get(anyString())).thenReturn(null);
        when(randomUtility.generate(SECRET_CODE_LENGTH)).thenReturn(mockVerificationCode);
        doNothing().when(sessionService).set(anyString(), anyString(), anyLong());

        // when
        String result = authService.sendEmailVerificationCode(mockVerificationCode);
        assertEquals(mockVerificationCode, result);
        verify(sessionService, times(1)).set(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("이메일 인증코드 검증")
    void verifyEmail() {
        // given
        String mockEmail = "test@gmail.com";
        String mockVerificationCode = "mock-verification-code";

        when(sessionService.get(anyString())).thenReturn(mockVerificationCode);

        // when & then
        assertDoesNotThrow(() -> authService.verifyEmail(mockEmail, mockVerificationCode));
        verify(sessionService, times(1)).delete(anyString());
    }

    @Test
    @DisplayName("중복으로 이메일 인증 요구시 예외(401)")
    void throwBadRequestByMultipleRequestVerificationCode() {
        // given
        String mockEmail = "test@gmail.com";
        String mockVerificationCode = "mock-verification-code";

        when(sessionService.get(anyString())).thenReturn(mockVerificationCode);

        // when
        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.sendEmailVerificationCode(mockEmail));
        assertEquals("ALREADY SENT EMAIL VERIFICATION CODE", exception.getMessage());
    }

    @Test
    @DisplayName("이메일 인증 코드 실패시")
    void failedToVerifyEmail() {
        // given
        String mockEmail = "test@gmail.com";
        String mockVerificationCode = "mock-verification-code";

        when(sessionService.get(anyString())).thenReturn(null);

        // when & then
        assertThrows(UnauthorizedException.class, () -> authService.verifyEmail(mockEmail, mockVerificationCode));
    }
}
