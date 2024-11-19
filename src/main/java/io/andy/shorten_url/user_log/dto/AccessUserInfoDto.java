package io.andy.shorten_url.user_log.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.UserLoginResponseDto;
import io.andy.shorten_url.user.dto.UserResponseDto;
import io.andy.shorten_url.user_log.constant.UserLogMessage;

import jakarta.validation.constraints.NotNull;

public record AccessUserInfoDto(
        @NotNull(message = "userId must be required")
        Long userId,
        @NotNull(message = "role must be required")
        UserRole role,
        @NotNull(message = "state must be required")
        UserState state,
        @NotNull(message = "log message must be required")
        UserLogMessage logMessage,
        String ipAddress,
        String userAgent
) {
        public static AccessUserInfoDto of(
                Long userId,
                UserRole role,
                UserState state,
                UserLogMessage logMessage,
                String ipAddress,
                String userAgent
        ) {
                return new AccessUserInfoDto(
                        userId,
                        role,
                        state,
                        logMessage,
                        ipAddress,
                        userAgent
                );
        }

        public static AccessUserInfoDto build(
                UserLoginResponseDto userDto,
                UserLogMessage logMessage,
                String ipAddress,
                String userAgent
        ) {
                return new AccessUserInfoDto(
                        userDto.id(),
                        userDto.role(),
                        userDto.state(),
                        logMessage,
                        ipAddress,
                        userAgent
                );
        }

        public static AccessUserInfoDto build(
                UserResponseDto userDto,
                UserLogMessage logMessage,
                String ipAddress,
                String userAgent
        ) {
                return new AccessUserInfoDto(
                        userDto.id(),
                        userDto.role(),
                        userDto.state(),
                        logMessage,
                        ipAddress,
                        userAgent
                );
        }
}