package io.andy.shorten_url.user.service;

import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.*;

import java.util.List;

public interface UserService {
    UserResponseDto createByEmail(CreateUserServiceDto userDto);
    UserLoginResponseDto login(UserLoginServiceDto userDto);
    UserLogoutResponseDto logout(UserLogoutRequestDto userDto);
    List<UserResponseDto> findAllUsers(List<UserState> states);
    long parseUserIdFromToken(String token);
    UserResponseDto findById(Long id);
    UserResponseDto findByUsername(String username);
    UserResponseDto updateUsernameById(Long id, String username);
    UserResponseDto updatePasswordById(Long id, String password);
    UserResponseDto updateStateById(Long id, UserState state);
    void deleteById(DeleteUserServiceDto userDto);
    boolean isDuplicateUsername(String username);
    void findPassword(FindPasswordDto findPasswordDto);
    String resetPassword(String username, String verificationCode);
    void sendEmailAuthCode(String recipient);
    void verifyEmail(String recipient, String verificationCode);
}
