package io.andy.shorten_url.user_log.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user_log.constant.UserLogMessage;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;

@Builder
public record UpdateInfoDto(
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
) { }
