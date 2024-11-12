package io.andy.shorten_url.user.dto;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;

public record UserLogOutDto (Long id, String ipAddress, String userAgent, String accessToken) {
    public UserLogOutDto(Long id, String ipAddress, String userAgent, TokenResponseDto authTokenDto) {
        this(id, ipAddress, userAgent, authTokenDto.accessToken());
    }
}
