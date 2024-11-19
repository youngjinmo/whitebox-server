package io.andy.shorten_url.auth.token.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateTokenDto {
    private final Long userId;
    private final String ipAddress;
    private final String userAgent;

    public static CreateTokenDto of(Long userId, String ipAddress, String userAgent) {
        return new CreateTokenDto(userId, ipAddress, userAgent);
    }

    public static CreateTokenDto from(VerifyTokenDto tokenDto) {
        return new CreateTokenDto(tokenDto.getUserId(), tokenDto.getIpAddress(), tokenDto.getUserAgent());
    }
}
