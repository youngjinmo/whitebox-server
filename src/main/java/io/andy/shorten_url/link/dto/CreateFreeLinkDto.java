package io.andy.shorten_url.link.dto;

public record CreateFreeLinkDto(
        String redirectionUrl,
        String ipAddress
) { }
