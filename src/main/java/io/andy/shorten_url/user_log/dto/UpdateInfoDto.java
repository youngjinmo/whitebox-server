package io.andy.shorten_url.user_log.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.UserResponseDto;
import io.andy.shorten_url.user_log.constant.UserLogMessage;

public record UpdateInfoDto(
        Long userId,
        UserState state,
        UserRole role,
        UserLogMessage message,
        String preValue,
        String postValue
) {
    public UpdateInfoDto(
            UserResponseDto userDto,
            UserLogMessage message,
            String preValue,
            String postValue
    ) {
        this(
                userDto.id(),
                userDto.state(),
                userDto.role(),
                message,
                preValue,
                postValue
        );
    }
}
