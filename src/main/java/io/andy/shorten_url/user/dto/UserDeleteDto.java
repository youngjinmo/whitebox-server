package io.andy.shorten_url.user.dto;

public record UserDeleteDto (
    Long id,
    String ipAddress,
    String userAgent
) {}
