package io.andy.shorten_url.user.dto;

public record UserLogOutDto (
    Long id,
    String ipAddress,
    String userAgent,
    String accessToken
){}
