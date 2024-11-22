package io.andy.shorten_url.user.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;

public record UserLogoutResponseDto(Long userId, UserRole role, UserState state) {
    public static UserLogoutResponseDto build(Long userId, UserRole role, UserState state) {
        return new UserLogoutResponseDto(userId, role, state);
    }
    public static UserLogoutResponseDto from(UserResponseDto userDto) {
        return new UserLogoutResponseDto(userDto.id(), userDto.role(), userDto.state());
    }
}
