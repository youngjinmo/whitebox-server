package io.andy.shorten_url.user.dto;

public record UserSignUpDto (
    String username,
    String ipAddress,
    String userAgent
) {}
