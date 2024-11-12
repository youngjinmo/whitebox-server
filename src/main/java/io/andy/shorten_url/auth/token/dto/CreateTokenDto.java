package io.andy.shorten_url.auth.token.dto;

import io.andy.shorten_url.user.dto.UserLoginRequestDto;

public record CreateTokenDto(Long userId, String userAgent, String ipAddress) {
    public CreateTokenDto(Long userId, UserLoginRequestDto userLoginDto) {
        this(userId, userLoginDto.userAgent(), userLoginDto.ipAddress());
    }
    public CreateTokenDto(VerifyTokenDto verifyTokenDto) {
        this(verifyTokenDto.userId(), verifyTokenDto.userAgent(), verifyTokenDto.ipAddress());
    }
}
