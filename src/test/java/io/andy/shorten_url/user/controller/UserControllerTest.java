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
//@Import(SecurityConfig.class)
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
        mockMvc.perform(get("/api/user/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .queryParam("recipient", email)
                    .queryParam("verificationCode", verificationCode))
                    .andExpect(status().isOk())
                    .andExpect(content().string("verified"));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout() throws Exception {
        // given
        Long userId = 1L;
        String username = "test@gmail.com";
        String givenPassword = "given_password";
        String ipAddress = "127.0.0.1";
        String userAgent = "test";

        User mockUser = new User(username, givenPassword, UserState.NEW, UserRole.USER);
        mockUser.setId(userId);
        UserResponseDto userResponseDto = UserResponseDto.from(mockUser);

        when(userService.findById(userId)).thenReturn(userResponseDto);
        doNothing().when(userService).logout(any(UserLogoutServiceDto.class));
        doNothing().when(userLogService).putUserAccessLog(any(AccessUserInfoDto.class));

        // when & then
        mockMvc.perform(delete("/api/user/{id}/logout", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer mock-access-token")
                    .header("X-Forwarded-For", ipAddress)
                    .header("User-Agent", userAgent))
                    .andExpect(status().isOk());
    }

    @Test
    void findUserById() throws Exception {
        // given

        // when & then

    }

    @Test
    void deleteUser() throws Exception {
    }

    @Test
    void resetPassword() throws Exception {
    }
}