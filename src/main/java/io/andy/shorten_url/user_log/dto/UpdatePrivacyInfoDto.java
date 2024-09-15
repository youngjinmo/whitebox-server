package io.andy.shorten_url.user_log.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.UserResponseDto;
import io.andy.shorten_url.user_log.constant.UserLogMessage;

public record UpdatePrivacyInfoDto(
        Long userId,
        UserState state,
        UserRole role,
        UserLogMessage message
) {
    public UpdatePrivacyInfoDto(UserResponseDto userDto, UserLogMessage message) {
        this(
                userDto.id(),
                userDto.state(),
                userDto.role(),
                message
        );
    }
}
