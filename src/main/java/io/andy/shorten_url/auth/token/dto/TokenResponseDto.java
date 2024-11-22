package io.andy.shorten_url.auth.token.dto;

public record TokenResponseDto(String accessToken, String refreshToken) {
    public static TokenResponseDto build(String accessToken, String refreshToken) {
       return new TokenResponseDto(accessToken, refreshToken);
    }
}
