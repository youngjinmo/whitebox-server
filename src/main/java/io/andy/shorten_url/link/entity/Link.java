package io.andy.shorten_url.link.entity;

import io.andy.shorten_url.link.constant.LinkState;

import jakarta.persistence.*;
import jakarta.validation.constraints.Null;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@ToString
public class Link {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private LinkState state;
    @Null
    private Long userId;
    private String urlPath;
    private String redirectionUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long accessCount;

    protected Link() {}

    public Link(Long userId, LinkState state, String urlPath, String redirectionUrl) {
        this.userId = userId;
        this.state = state;
        this.urlPath = urlPath;
        this.redirectionUrl = redirectionUrl;
    }

    public Link(LinkState state, String urlPath, String redirectionUrl) {
        this.state = state;
        this.urlPath = urlPath;
        this.redirectionUrl = redirectionUrl;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        accessCount = 0L;
    }
}
