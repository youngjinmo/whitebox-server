package io.andy.shorten_url.user.dto;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;

public record UserLogoutServiceDto(Long id, String accessToken) {
    public static UserLogoutServiceDto of(Long id, String token) {
        return new UserLogoutServiceDto(id, token);
    }
    public static UserLogoutServiceDto build(Long userId, TokenResponseDto tokenDto) {
        return new UserLogoutServiceDto(userId, tokenDto.accessToken());
    }
}
