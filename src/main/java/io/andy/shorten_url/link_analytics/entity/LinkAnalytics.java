package io.andy.shorten_url.link_analytics.entity;

import io.andy.shorten_url.link_analytics.dto.PutAccessLogDto;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@Entity
@Getter
public class LinkAnalytics {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime createdAt;
    private Long linkId;
    private String ipAddress;
    private String userAgent;
    private String location;
    private String referer;

    protected LinkAnalytics() {}

    public LinkAnalytics(Long linkId, PutAccessLogDto accessLogDto) {
        this.linkId = linkId;
        this.ipAddress = accessLogDto.getIpAddress();
        this.userAgent = accessLogDto.getUserAgent();
        this.location = accessLogDto.getLocation();
        this.referer = accessLogDto.getReferer();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Profile("test")
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
