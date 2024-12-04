package io.andy.shorten_url.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.entity.User;
import io.andy.shorten_url.user.service.UserService;
import io.andy.shorten_url.user_log.dto.AccessUserInfoDto;
import io.andy.shorten_url.user_log.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.user_log.service.UserLogService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean UserService userService;
    @MockBean UserLogService userLogService;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signUp() throws Exception {
        // given
        Long userId = 1L;
        String username = "test@gmail.com";
        String givenPassword = "given_password";
        String ipAddress = "127.0.0.1";
        String userAgent = "test";
        User mockUser = new User(username, givenPassword, UserState.NEW, UserRole.USER);
        mockUser.setId(userId);

        CreateUserRequestDto createUserDto = new CreateUserRequestDto(username, givenPassword);
        UserResponseDto userResponseDto = UserResponseDto.from(mockUser);

        when(userService.createByEmail(any(CreateUserServiceDto.class))).thenReturn(userResponseDto);
        doNothing().when(userLogService).putUserAccessLog(any(AccessUserInfoDto.class));

        // when & then
        mockMvc.perform(post("/api/user/create")
                    .header("X-Forwarded-For", ipAddress)
                    .header("User-Agent", userAgent)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createUserDto)))
                    .andExpect(status().isCreated())
                    .andExpect(content().json(objectMapper.writeValueAsString(userResponseDto)));
    }

    @Test
    @DisplayName("중복된 이메일로 회원가입 실패 테스트")
    void failedSignUpByBadRequest() throws Exception {
        // given
        String username = "test@gmail.com";
        String givenPassword = "given_password";
        String ipAddress = "127.0.0.1";
        String userAgent = "test";

        CreateUserRequestDto createUserDto = new CreateUserRequestDto(username, givenPassword);

        when(userService.createByEmail(any(CreateUserServiceDto.class))).thenThrow(BadRequestException.class);
        doNothing().when(userLogService).putUserAccessLog(any(AccessUserInfoDto.class));

        // when & then
        mockMvc.perform(post("/api/user/create")
                    .header("X-Forwarded-For", ipAddress)
                    .header("User-Agent", userAgent)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createUserDto)))
                    .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login() throws Exception {
        // given
        Long userId = 1L;
        String username = "test@gmail.com";
        String givenPassword = "given_password";
        String ipAddress = "127.0.0.1";
        String userAgent = "test";

        User mockUser = new User(username, givenPassword, UserState.NEW, UserRole.USER);
        mockUser.setId(userId);

        UserLoginRequestDto loginUserDto = new UserLoginRequestDto(username, givenPassword);
        UserLoginResponseDto userResponseDto = UserLoginResponseDto.build(mockUser,
                new TokenResponseDto("mockAccessToken", "mockRefreshToken"));

        when(userService.login(any(UserLoginServiceDto.class))).thenReturn(userResponseDto);
        doNothing().when(userLogService).putUserAccessLog(any(AccessUserInfoDto.class));

        // when & then
        mockMvc.perform(post("/api/user/login")
                    .header("X-Forwarded-For", ipAddress)
                    .header("User-Agent", userAgent)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(userResponseDto)));
    }

    @Test
    @DisplayName("이메일 인증 실패시 예외 검증")
    void failedLogin() throws Exception {
        // given
        String username = "test@gmail.com";
        String givenPassword = "given_password";
        String ipAddress = "127.0.0.1";
        String userAgent = "test";

        UserLoginRequestDto loginUserDto = new UserLoginRequestDto(username, givenPassword);

        when(userService.login(any(UserLoginServiceDto.class))).thenThrow(new UnauthorizedException("INVALID PASSWORD"));

        // when & then
        mockMvc.perform(post("/api/user/login")
                    .header("X-Forwarded-For", ipAddress)
                    .header("User-Agent", userAgent)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginUserDto)))
                    .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("이메일 인증")
    void sendEmailAuth() throws Exception {
        // given
        String email = "test@gmail.com";

        when(userService.isDuplicateUsername(email)).thenReturn(false);
        doNothing().when(userService).sendEmailAuthCode(email);

        // when & then
        mockMvc.perform(post("/api/user/email-verification-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(email))
                    .andExpect(status().isOk());
    }

    @Test
    @DisplayName("이메일 인증 코드 검증")
    void verifyEmailAuth() throws Exception {
        // given
        String email = "test@gmail.com";
        String verificationCode = "VERIFY";

        doNothing().when(userService).verifyEmail(email, verificationCode);

        // when & then
        mockMvc.perform(post("/api/user/verify/email")
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("verificationCode", verificationCode)
                .queryParam("recipient", email))
                .andExpect(status().isOk())
                .andExpect(content().string("verified"));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout() throws Exception {
        // given
        Long userId = 1L;
        UserRole role = UserRole.USER;
        UserState state = UserState.NORMAL;

        when(userService.logout(any(UserLogoutRequestDto.class)))
                .thenReturn(UserLogoutResponseDto.build(userId, role, state));
        doNothing().when(userLogService).putUserAccessLog(any(AccessUserInfoDto.class));

        // when & then
        mockMvc.perform(delete("/api/user/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer mock-access-token")
                    .header("X-Forwarded-For", "127.0.0.1")
                    .header("User-Agent", "test"))
                    .andExpect(status().isOk());
    }

    @Test
    @DisplayName("user 조회 (by id)")
    void findUserById() throws Exception {
        // given
        Long userId = 1L;
        User mockUser = new User("test@gmail.com", "given-password", UserState.NORMAL, UserRole.USER);
        mockUser.setId(userId);

        when(userService.findById(userId)).thenReturn(UserResponseDto.from(mockUser));

        // when & then
        mockMvc.perform(get("/api/user/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer mock-access-token")
                        .header("X-Forwarded-For", "127.0.0.1")
                        .header("User-Agent", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("탈퇴 신청 회원")
    void withdrawUser() throws Exception {
        // given
        Long userId = 1L;
        User mockUser = new User("test@gmail.com", "given-password", UserState.NORMAL, UserRole.USER);
        mockUser.setId(userId);

        when(userService.parseUserIdFromToken(anyString())).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(UserResponseDto.from(mockUser));
        when(userService.updateStateById(userId, UserState.WITHDRAWN)).thenReturn(UserResponseDto.from(mockUser));
        doNothing().when(userLogService).putUpdateInfoLog(any(UpdatePrivacyInfoDto.class));

        // when & then
        mockMvc.perform(delete("/api/user/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer mock-access-token")
                .header("X-Forwarded-For", "127.0.0.1")
                .header("User-Agent", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 초기화 위한 이메일 인증코드 발송")
    void authenticateResetPassword() throws Exception {
        // given
        String username = "test@gmail.com";

        doNothing().when(userService).findPassword(any(FindPasswordDto.class));

        // when & then
        mockMvc.perform(post("/api/user/find-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(username))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 초기화")
    void resetPassword() throws Exception {
        // given
        String mockUsername = "test@gmail.com";
        String mockVerificationCode = "VERIFY";
        String newPassword = "newPassword";

        User mockUser = new User(mockUsername, "given-password", UserState.NORMAL, UserRole.USER);
        mockUser.setId(1L);

        when(userService.resetPassword(anyString(), anyString())).thenReturn(newPassword);
        when(userService.findByUsername(mockUsername)).thenReturn(UserResponseDto.from(mockUser));
        doNothing().when(userLogService).putUpdateInfoLog(any(UpdatePrivacyInfoDto.class));

        // when & then
        mockMvc.perform(patch("/api/user/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer mock-access-token")
                        .header("X-Forwarded-For", "127.0.0.1")
                        .header("User-Agent", "test")
                        .queryParam("username", mockUsername)
                        .queryParam("verificationCode", mockVerificationCode))
                .andExpect(status().isOk())
                .andExpect(content().string(newPassword));
    }
}