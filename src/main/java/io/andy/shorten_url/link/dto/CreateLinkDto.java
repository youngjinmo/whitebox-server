package io.andy.shorten_url.link.dto;

public record CreateLinkDto(
        Long userId,
        String redirectionUrl
) { }
