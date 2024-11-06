package io.andy.shorten_url.auth.token.dto;

public record CreateAuthTokenRequestDto(Long userId, String userAgent) { }
