package io.andy.shorten_url.auth.token.dto;

public record VerifyAuthTokenDto(Long userId, String userAgent, String token) { }
