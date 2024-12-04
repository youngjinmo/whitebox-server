package io.andy.shorten_url.auth.token.dto;

import lombok.Getter;

@Getter
public class CreateTokenDto extends TokenRequestDto {
    private final long tokenLiveTime;

    public CreateTokenDto(Long userId, String ipAddress, String userAgent, long tokenLiveTime) {
        super(userId, ipAddress, userAgent);
        this.tokenLiveTime = tokenLiveTime;
    }

    public static CreateTokenDto build(Long userId, String ipAddress, String userAgent, long tokenLiveTime) {
        return new CreateTokenDto(userId, ipAddress, userAgent, tokenLiveTime);
    }

    public static CreateTokenDto of(TokenRequestDto tokenRequestDto, long tokenLiveTime) {
        return new CreateTokenDto(tokenRequestDto.getUserId(), tokenRequestDto.getIpAddress(), tokenRequestDto.getUserAgent(), tokenLiveTime);
    }
}
