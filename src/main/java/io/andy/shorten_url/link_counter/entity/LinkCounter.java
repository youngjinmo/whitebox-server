package io.andy.shorten_url.link_counter.entity;

import io.andy.shorten_url.link_counter.dto.PutAccessLogDto;

import jakarta.persistence.*;

import lombok.Getter;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;

@Entity
@Getter
public class LinkCounter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime createdAt;
    private Long linkId;
    private String ipAddress;
    private String userAgent;
    private String location;
    private String referer;

    public LinkCounter() {}

    public LinkCounter(Long linkId, PutAccessLogDto accessLogDto) {
        this.linkId = linkId;
        this.createdAt = LocalDateTime.now();
        this.ipAddress = accessLogDto.getIpAddress();
        this.userAgent = accessLogDto.getUserAgent();
        this.location = accessLogDto.getLocation();
        this.referer = accessLogDto.getReferer();
    }

    @Profile("test")
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
