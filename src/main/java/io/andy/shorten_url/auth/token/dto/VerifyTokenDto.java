package io.andy.shorten_url.auth.token.dto;

import lombok.Getter;

@Getter
public class VerifyTokenDto extends TokenRequestDto {
    private final String token;

    private VerifyTokenDto(Long userId, String ipAddress, String userAgent, String token) {
        super(userId, ipAddress, userAgent);
        this.token = token;
    }

    public static VerifyTokenDto build(Long userId, String ipAddress, String userAgent, String token) {
        return new VerifyTokenDto(userId, ipAddress, userAgent, token);
    }
}
