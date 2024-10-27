package io.andy.shorten_url.link_analytics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Builder
public class PutAccessLogDto {
    private String ipAddress;
    private String location;
    private String userAgent;
    private String referer;
}
