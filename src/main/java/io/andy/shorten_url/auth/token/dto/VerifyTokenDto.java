package io.andy.shorten_url.auth.token.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class VerifyTokenDto extends CreateTokenDto {
    private final String token;
    public VerifyTokenDto(Long userId, String ipAddress, String userAgent, String token) {
        super(userId, ipAddress, userAgent);
        this.token = token;
    }
    public static VerifyTokenDto of(Long userId, String ipAddress, String userAgent, String token) {
        return new VerifyTokenDto(userId, ipAddress, userAgent, token);
    }
}
