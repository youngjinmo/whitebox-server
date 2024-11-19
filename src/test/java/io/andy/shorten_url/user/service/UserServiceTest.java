package io.andy.shorten_url.user.service;

import io.andy.shorten_url.auth.AuthService;
import io.andy.shorten_url.auth.token.dto.CreateTokenDto;
import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.entity.User;
import io.andy.shorten_url.user.repository.UserRepository;
import io.andy.shorten_url.util.mail.MailService;
import io.andy.shorten_url.util.mail.dto.MailMessageDto;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.andy.shorten_url.auth.AuthPolicy.RESET_PASSWORD_LENGTH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private MailService mailService;
    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;
    @InjectMocks private UserServiceImpl userService;

    @Test
    @DisplayName("회원가입 정상 동작 확인")
    void createByEmail() {
        // given
        String username = "test@yj.com";
        String givenPassword = "given_password";
        String encodedPassword = "encoded_password";
        User mockUser = new User(username, encodedPassword, UserState.NEW, UserRole.USER);
        mockUser.setId(1L);

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(authService.encodePassword(givenPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // when
        UserResponseDto result = userService.createByEmail(CreateUserServiceDto.of(username, givenPassword));

        // then
        assertNotNull(result);
        assertEquals(mockUser.getUsername(), result.username());
        verify(authService, times(1)).encodePassword(givenPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("기가입된 이메일로 회원가입 시도시 예외처리")
    void throwExceptionWithDuplicatedUsername() {
        // given
        String username = "test@yj.com";
        String givenPassword = "given_password";
        User mockUser = new User(username, "encoded_password", UserState.NEW, UserRole.USER);
        mockUser.setId(1L);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        // when
        BadRequestException exception = assertThrows(
                BadRequestException.class, () -> userService.createByEmail(CreateUserServiceDto.of(username, givenPassword))
        );

        // then
        assertEquals("DUPLICATE USERNAME", exception.getMessage());
        verify(authService, times(0)).encodePassword(givenPassword);
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 정상 동작 확인")
    void login() {
        // given
        String username = "test@yj.com";
        String givenPassword = "given_password";
        String ipAddress = "127.0.0.1";
        String userAgent = "chrome";
        String encodedPassword = "encoded_password";

        User mockUser = new User(username, encodedPassword, UserState.NEW, UserRole.USER);
        mockUser.setId(1L);
        TokenResponseDto tokenResponseDto = new TokenResponseDto("accessToken", "refreshToken");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        when(authService.matchPassword(givenPassword, encodedPassword)).thenReturn(true);
        when(authService.grantAuthToken(any(CreateTokenDto.class))).thenReturn(tokenResponseDto);

        // when
        UserLoginResponseDto result = userService.login(UserLoginServiceDto.of(username, givenPassword, ipAddress, userAgent));

        // then
        assertNotNull(result);
        assertEquals(mockUser.getUsername(), result.username());
        verify(authService, times(1)).matchPassword(givenPassword, encodedPassword);
    }

    @Test
    @DisplayName("이메일 틀렸을때 예외 확인")
    void throwExceptionWithWrongUsername() {
        // given
        String username = "test@yj.com";
        String givenPassword = "given_password";
        String ipAddress = "127.0.0.1";
        String userAgent = "chrome";
        String encodedPassword = "encoded_password";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // when
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                userService.login(UserLoginServiceDto.of(username, givenPassword, ipAddress, userAgent))
        );

        // then
        assertEquals("INVALID USERNAME", exception.getMessage());
        verify(authService, times(0)).matchPassword(givenPassword, encodedPassword);
    }

    @Test
    @DisplayName("패스워드 틀렸을때 예외 확인")
    void throwExceptionWithWrongPassword() {
        // given
        String username = "test@yj.com";
        String givenPassword = "given_password";
        String encodedPassword = "encoded_password";
        String ipAddress = "127.0.0.1";
        String userAgent = "chrome";

        User mockUser = new User(username, encodedPassword, UserState.NEW, UserRole.USER);
        mockUser.setId(1L);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        when(authService.matchPassword(givenPassword, encodedPassword)).thenReturn(false);

        // when
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                userService.login(UserLoginServiceDto.of(username, givenPassword, ipAddress, userAgent))
        );

        // then
        assertEquals("INVALID PASSWORD", exception.getMessage());
        verify(authService, times(1)).matchPassword(givenPassword, encodedPassword);
    }

    @Test
    @DisplayName("로그아웃 동작 확인")
    void logout() {
        // given
        Long userId = 1L;
        String mockToken = "access-token";

        User mockUser = new User("test@yj.com", "password", UserState.NEW, UserRole.USER);
        mockUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        doNothing().when(authService).revokeAuthToken(userId, mockToken);

        // when
        userService.logout(UserLogoutServiceDto.of(userId, mockToken));

        // then
        verify(authService, times(1)).revokeAuthToken(userId, mockToken);
    }

    @Test
    @DisplayName("전체 회원 조회 동작 확인")
    void findAllUsers() {
        // given
        List<User> mockUsers = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            User mockUser = new User(i+"-test@yj.com", "password", UserState.NEW, UserRole.USER);
            mockUser.setId(Long.valueOf(i));
            mockUsers.add(mockUser);
        }

        when(userRepository.findByStateIn(List.of(UserState.NEW))).thenReturn(mockUsers);

        // when
        List<UserResponseDto> newUsers = userService.findAllUsers(List.of(UserState.NEW));
        List<UserResponseDto> normalUsers = userService.findAllUsers(List.of(UserState.NORMAL));

        // then
        assertEquals(2, newUsers.size());
        assertEquals(0, normalUsers.size());
        verify(userRepository, times(1)).findByStateIn(List.of(UserState.NEW));
        verify(userRepository, times(1)).findByStateIn(List.of(UserState.NORMAL));
    }

    @Test
    @DisplayName("id기반으로 회원조회")
    void findById() {
        // given
        Long userId = 1L;
        User user = new User("test@yj.com", "password", UserState.NEW, UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        UserResponseDto result = userService.findById(userId);

        // then
        assertNotNull(result);
        assertEquals(user.getUsername(), result.username());
    }

    @Test
    @DisplayName("존재하지 않는 id로 회원조회시 예외 확인")
    void throwExceptionWithNotExistUserId() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        NotFoundException result = assertThrows(NotFoundException.class, () -> userService.findById(userId));

        // then
        assertEquals("USER NOT FOUND", result.getMessage());
    }

    @Test
    @DisplayName("이메일 기반으로 회원 조회")
    void findByUsername() {
        // given
        Long userId = 1L;
        User user = new User("test-user", "password", UserState.NEW, UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        UserResponseDto result = userService.findById(userId);

        // then
        assertNotNull(result);
        assertEquals(user.getUsername(), result.username());
    }

    @Test
    @DisplayName("존재하지 않는 username 로 회원조회시 예외 확인")
    void throwExceptionWithNotExistUsername() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        NotFoundException result = assertThrows(NotFoundException.class, () -> userService.findById(userId));

        // then
        assertEquals("USER NOT FOUND", result.getMessage());
    }

    @Test
    @DisplayName("id 기반으로 이메일 수정")
    void updateUsernameById() {
        // given
        Long userId = 1L;
        String username = "test@tj.com";
        String newUsername = "test@yj.com";
        User user = new User(username, "password", UserState.NEW, UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(newUsername)).thenReturn(Optional.empty());

        // when
        UserResponseDto result = userService.updateUsernameById(userId, newUsername);

        // then
        assertNotNull(result);
        assertEquals(newUsername, result.username());
        assertNotNull(result.updatedAt());
        verify(userRepository, times(1)).findByUsername(newUsername);
    }

    @Test
    @DisplayName("존재하지 않는 id로 이메일 수정 시도시 예외 확인")
    void updateUsernameWithNotExistId() {
        // given
        Long userId = 1L;
        String newUsername = "test@yj.com";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        NotFoundException result = assertThrows(NotFoundException.class, () -> userService.updateUsernameById(userId, newUsername));

        // then
        assertEquals("FAILED TO UPDATE USERNAME BY INVALID ID", result.getMessage());
        verify(userRepository, times(0)).findByUsername(newUsername);
    }

    @Test
    @DisplayName("id 기반으로 비밀번호 수정")
    void updatePasswordById() {
        // given
        Long userId = 1L;
        String newPassword = "strong-password";
        User user = new User("test@yj.com", "password", UserState.NEW, UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authService.encodePassword(newPassword)).thenReturn("encoded-password");

        // when
        UserResponseDto result = userService.updatePasswordById(userId, newPassword);

        // then
        assertNotNull(result);
        assertNotNull(result.updatedAt());
        verify(authService, times(1)).encodePassword(newPassword);
    }

    @Test
    @DisplayName("상태 수정 동작 확인")
    void updateStateById() {
        // given
        Long userId = 1L;
        UserState newState = UserState.NORMAL;
        User user = new User("test@yj.com", "password", UserState.NEW, UserRole.USER);
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        UserResponseDto result = userService.updateStateById(userId, newState);

        // then
        assertNotNull(result);
        assertEquals(newState, result.state());
        assertNotNull(result.updatedAt());
    }

    @Test
    @DisplayName("탈퇴 기능 확인")
    void deleteById() {
        // given
        Long userId = 1L;
        User mockUser = new User("test@yj.com", "password", UserState.NORMAL, UserRole.USER);
        mockUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        doNothing().when(authService).revokeAllSessionsByUserId(userId);

        // when
        userService.deleteById(DeleteUserServiceDto.of(userId, "127.0.0.1", "chrome"));

        // then
        verify(authService, times(1)).revokeAllSessionsByUserId(userId);
    }

    @Test
    @DisplayName("패스워드 리셋")
    void resetPassword() throws MessagingException {
        // given
        Long userId = 1L;
        User user = new User("test@yj.com", "password", UserState.NEW, UserRole.USER);
        user.setId(userId);
        String expected = "temp-password";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authService.generateResetPassword(RESET_PASSWORD_LENGTH)).thenReturn(expected);
        when(mailService.createMailMessage(any(MailMessageDto.class))).thenReturn(mock(MimeMessage.class));
        doNothing().when(mailService).sendMail(anyString(), any(MimeMessage.class));

        // when
        String actual = userService.resetPassword(userId);

        // then
        assertEquals(expected, actual);
        verify(authService, times(1)).generateResetPassword(RESET_PASSWORD_LENGTH);
    }
}