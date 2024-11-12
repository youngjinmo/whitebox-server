package io.andy.shorten_url.auth.token.dto;

public record VerifyTokenDto(
        Long userId,
        String userAgent,
        String ipAddress,
        String token
) {
    public VerifyTokenDto(CreateTokenDto tokenDto, String token) {
        this(tokenDto.userId(), tokenDto.userAgent(), tokenDto.ipAddress(), token);
    }
}
