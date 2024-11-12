package io.andy.shorten_url.user.dto;

import java.time.LocalDateTime;

public record UserLoginRequestDto(
        String username,
        String ipAddress,
        String userAgent,
        LocalDateTime lastLoginAt
) {
    public UserLoginRequestDto(String username, String ipAddress, String userAgent) {
        this(username, ipAddress, userAgent, LocalDateTime.now());
    }
}