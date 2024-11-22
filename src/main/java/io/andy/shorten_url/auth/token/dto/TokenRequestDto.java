package io.andy.shorten_url.auth.token.dto;

import lombok.Getter;

@Getter
public class TokenRequestDto {
    private final Long userId;
    private final String ipAddress;
    private final String userAgent;

    public TokenRequestDto(Long userId, String ipAddress, String userAgent) {
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public static TokenRequestDto build(Long userId, String ipAddress, String userAgent) {
        return new TokenRequestDto(userId, ipAddress, userAgent);
    }

    public static TokenRequestDto from(VerifyTokenDto tokenDto) {
        return new TokenRequestDto(tokenDto.getUserId(), tokenDto.getIpAddress(), tokenDto.getUserAgent());
    }
}
