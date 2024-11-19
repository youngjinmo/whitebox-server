package io.andy.shorten_url.user_log.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.UserResponseDto;
import io.andy.shorten_url.user_log.constant.UserLogMessage;

import jakarta.validation.constraints.NotNull;

public record UpdateUserInfoDto(
        @NotNull(message = "userId must be required")
        Long userId,
        @NotNull(message = "state must be required")
        UserState state,
        @NotNull(message = "role must be required")
        UserRole role,
        @NotNull(message = "log message must be required")
        UserLogMessage message,
        String preValue,
        String postValue
) {
        public static UpdateUserInfoDto build(
                UserResponseDto userDto,
                UserLogMessage logMessage,
                String preValue,
                String postValue
        ) {
                return new UpdateUserInfoDto(
                        userDto.id(),
                        userDto.state(),
                        userDto.role(),
                        logMessage,
                        preValue,
                        postValue
                );
        }
}
