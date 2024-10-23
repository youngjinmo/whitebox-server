package io.andy.shorten_url.link_analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class PutAccessLogDto {
    private String ipAddress;
    private String location;
    private String userAgent;
    private String referer;
}
