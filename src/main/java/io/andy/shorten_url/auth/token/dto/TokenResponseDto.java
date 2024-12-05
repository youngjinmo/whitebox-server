package io.andy.shorten_url.auth.token.dto;

public record TokenResponseDto(String accessToken, String refreshToken) {
    public static TokenResponseDto build(String accessToken, String refreshToken) {
        if (accessToken.isEmpty()) {
            throw new IllegalArgumentException("Access token cannot be empty");
        }
        if (refreshToken.isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be empty");
        }
       return new TokenResponseDto(accessToken, refreshToken);
    }
}
