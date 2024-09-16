package io.andy.shorten_url.link_counter.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PutAccessLogDto {
    private String ipAddress;
    private String location;
    private String userAgent;
    private String referer;

    public PutAccessLogDto(
            String ipAddress,
            String location,
            String userAgent,
            String referer
    ) {
        this.ipAddress = ipAddress;
        this.location = location;
        this.userAgent = userAgent;
        this.referer = referer;
    }
}
