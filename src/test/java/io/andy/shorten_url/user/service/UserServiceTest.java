package io.andy.shorten_url.user.service;

import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.UserLoginDto;
import io.andy.shorten_url.user.dto.UserResponseDto;
import io.andy.shorten_url.user.dto.UserSignUpDto;
import io.andy.shorten_url.user.entity.User;
import io.andy.shorten_url.user.repository.UserRepository;
import io.andy.shorten_url.userLog.dto.AccessInfoDto;
import io.andy.shorten_url.userLog.dto.UpdateInfoDto;
import io.andy.shorten_url.userLog.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.userLog.service.UserLogService;

import org.junit.jupiter.api.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
class UserServiceTest {
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserRepository userRepository;
    @Mock private UserLogService userLogService;
    @InjectMocks private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("회원가입 정상 동작 확인")
    void createUserByUsername() {
        // given
        UserSignUpDto userSignUpDto = new UserSignUpDto("test@yj.com", "127.0.0.1", "test-agent");
        String givenPassword = "given_password";
        String encodedPassword = "encoded_password";
        User user = new User(userSignUpDto.username(), encodedPassword, UserState.NEW, UserRole.USER);

        when(userRepository.findByUsername(userSignUpDto.username())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(givenPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(userLogService).putUserAccessLog(any(AccessInfoDto.class));

        // when
        UserResponseDto result = userService.createUserByUsername(userSignUpDto, givenPassword);

        // then
        assertNotNull(result);
        assertEquals(user.getUsername(), result.username());
        verify(passwordEncoder, times(1)).encode(givenPassword);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("기가입된 이메일로 회원가입 시도시 예외처리")
    void throwExceptionWithDuplicatedUsername() {
        // given
        UserSignUpDto userSignUpDto = new UserSignUpDto("test@yj.com", "127.0.0.1", "test-agent");
        String givenPassword = "given_password";
        User user = new User(userSignUpDto.username(), "encoded_password", UserState.NEW, UserRole.USER);

        when(userRepository.findByUsername(userSignUpDto.username())).thenReturn(Optional.of(user));

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> userService.createUserByUsername(userSignUpDto, givenPassword));

        // then
        assertEquals("DUPLICATE USERNAME", exception.getMessage());
        verify(passwordEncoder, times(0)).encode(givenPassword);
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 정상 동작 확인")
    void login() {
        // given
        UserLoginDto userLoginDto = new UserLoginDto("test@yj.com", "127.0.0.1", "test-agent");
        String givenPassword = "given_password";
        String encodedPassword = "encoded_password";
        User user = new User(userLoginDto.username(), encodedPassword, UserState.NEW, UserRole.USER);

        when(userRepository.findByUsername(userLoginDto.username())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(givenPassword, encodedPassword)).thenReturn(true);
        doNothing().when(userLogService).putUserAccessLog(any(AccessInfoDto.class));

        // when
        UserResponseDto result = userService.login(userLoginDto, givenPassword);

        // then
        assertNotNull(result);
        assertEquals(user.getUsername(), result.username());
        verify(passwordEncoder, times(1)).matches(givenPassword, encodedPassword);
    }

    @Test
    @DisplayName("이메일 틀렸을때 예외 확인")
    void throwExceptionWithWrongUsername() {
        // given
        UserLoginDto userLoginDto = new UserLoginDto("test@yj.com", "127.0.0.1", "test-agent");
        String givenPassword = "given_password";
        String encodedPassword = "encoded_password";

        when(userRepository.findByUsername(userLoginDto.username())).thenReturn(Optional.empty());

        // when
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.login(userLoginDto, givenPassword));

        // then
        assertEquals("INVALID USERNAME", exception.getMessage());
        verify(passwordEncoder, times(0)).matches(givenPassword, encodedPassword);
        verify(userLogService, times(0)).putUserAccessLog(any(AccessInfoDto.class));
    }

    @Test
    @DisplayName("패스워드 틀렸을때 예외 확인")
    void throwExceptionWithWrongPassword() {
        // given
        UserLoginDto userLoginDto = new UserLoginDto("test@yj.com", "127.0.0.1", "test-agent");
        String givenPassword = "given_password";
        String encodedPassword = "encoded_password";
        User user = new User(userLoginDto.username(), encodedPassword, UserState.NEW, UserRole.USER);

        when(userRepository.findByUsername(userLoginDto.username())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(givenPassword, encodedPassword)).thenReturn(false);

        // when
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> userService.login(userLoginDto, givenPassword));

        // then
        assertEquals("INVALID PASSWORD", exception.getMessage());
        verify(passwordEncoder, times(1)).matches(givenPassword, encodedPassword);
        verify(userLogService, times(0)).putUserAccessLog(any(AccessInfoDto.class));
    }

    @Test
    @DisplayName("로그아웃 동작 확인")
    @Disabled
    void logout() {}

    @Test
    @DisplayName("전체 회원 조회 동작 확인")
    void findAllUsers() {
        // given
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            users.add(new User(i+"-test@yj.com", "password", UserState.NEW, UserRole.USER));
        }
        when(userRepository.findAll()).thenReturn(users);

        // when
        List<UserResponseDto> result = userService.findAllUsers();

        // then
        assertNotNull(result);
        assertEquals(10, result.size());
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
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded-password");
        doNothing().when(userLogService).putUpdateInfoLog(any(UpdatePrivacyInfoDto.class));

        // when
        UserResponseDto result = userService.updatePasswordById(userId, newPassword);

        // then
        assertNotNull(result);
        verify(passwordEncoder, times(1)).encode(newPassword);
    }

    @Test
    @DisplayName("상태 수정 동작 확인")
    void updateStateById() {
        // given
        Long userId = 1L;
        UserState newState = UserState.NORMAL;
        User user = new User("test@yj.com", "password", UserState.NEW, UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doNothing().when(userLogService).putUpdateInfoLog(any(UpdateInfoDto.class));

        // when
        UserResponseDto result = userService.updateStateById(userId, newState);

        // then
        assertNotNull(result);
        assertEquals(newState, result.state());
        assertNotNull(result.updatedAt());
    }

    @Test
    @Disabled
    void deleteById() {
    }
}