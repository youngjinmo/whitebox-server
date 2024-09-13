package io.andy.shorten_url.link.entity;

import io.andy.shorten_url.link.constant.LinkState;

import jakarta.persistence.*;

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
    private Long userId;
    private String urlPath;
    private String redirectionUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long accessCount;

    public Link() {}

    public Link(
            Long userId,
            LinkState state,
            String urlPath,
            String redirectionUrl
    ) {
        this.userId = userId;
        this.state = state;
        this.urlPath = urlPath;
        this.redirectionUrl = redirectionUrl;
        this.accessCount = 0L;
        this.createdAt = LocalDateTime.now();
    }
}
