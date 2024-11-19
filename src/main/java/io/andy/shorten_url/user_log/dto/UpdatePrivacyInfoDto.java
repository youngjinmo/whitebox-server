package io.andy.shorten_url.user_log.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.UserResponseDto;
import io.andy.shorten_url.user_log.constant.UserLogMessage;

import jakarta.validation.constraints.NotNull;

public record UpdatePrivacyInfoDto(
        @NotNull(message = "userId must be required by updated privacy info")
        Long userId,
        @NotNull(message = "state must be required by updated privacy info")
        UserState state,
        @NotNull(message = "role must be required by updated privacy info")
        UserRole role,
        @NotNull(message = "log message must be required by updated privacy info")
        UserLogMessage message,
        @NotNull(message = "ip address must be required by updated privacy info")
        String ipAddress,
        @NotNull(message = "user agent must be required by updated privacy info")
        String userAgent
) {
        public static UpdateUserInfoDto build(
                UserResponseDto userDto,
                UserLogMessage logMessage,
                String ipAddress,
                String userAgent
        ) {
                return new UpdateUserInfoDto(
                        userDto.id(),
                        userDto.state(),
                        userDto.role(),
                        logMessage,
                        ipAddress,
                        userAgent
                );
        }
}
