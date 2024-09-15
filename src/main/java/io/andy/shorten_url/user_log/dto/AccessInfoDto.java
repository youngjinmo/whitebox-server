package io.andy.shorten_url.user_log.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.UserResponseDto;
import io.andy.shorten_url.user_log.constant.UserLogMessage;

public record AccessInfoDto(
        Long userId,
        UserState state,
        UserRole role,
        UserLogMessage message,
        String ipAddress,
        String userAgent
) {
    public AccessInfoDto(
            UserResponseDto userDto,
            UserLogMessage message,
            String ipAddress,
            String userAgent
    ) {
        this(
                userDto.id(),
                userDto.state(),
                userDto.role(),
                message,
                ipAddress,
                userAgent
        );
    }
}
