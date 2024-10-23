package io.andy.shorten_url.link.controller;

import io.andy.shorten_url.link.constant.LinkState;
import io.andy.shorten_url.link.dto.CreateLinkDto;
import io.andy.shorten_url.link.entity.Link;
import io.andy.shorten_url.link.service.LinkService;
import io.andy.shorten_url.link_analytics.dto.PutAccessLogDto;
import io.andy.shorten_url.link_analytics.service.LinkAnalyticsService;
import io.andy.shorten_url.util.ClientMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
public class LinkController {
    private final LinkService linkService;
    private final LinkAnalyticsService linkAnalyticsService;

    @Autowired
    public LinkController(LinkService linkService, LinkAnalyticsService linkAnalyticsService) {
        this.linkService = linkService;
        this.linkAnalyticsService = linkAnalyticsService;
    }

    @PostMapping("/link/create")
    public Link createLink(@RequestBody CreateLinkDto createLinkDto) {
        Link link = linkService.createLink(createLinkDto);
        log.info("Created link: {}", link);
        return link;
    }

    @GetMapping("/link/all")
    public List<Link> findAllLinks(Pageable pageable) {
        return linkService.findAllLinks(pageable);
    }

    @GetMapping(value = {"/{urlPath}", "/{urlPath}/"})
    public void redirectUrl(HttpServletRequest request, HttpServletResponse response, @PathVariable String urlPath) throws IOException {
        Link link = linkService.findLinkByUrlPath(urlPath);
        if (!link.getState().equals(LinkState.PUBLIC)) {
            String clientIp = ClientMapper.parseClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            log.debug("접근 불가능한 링크에 대한 접근 시도입니다. link_id={}, link_state={}, ip={}, userAgent={}", link.getId(), link.getState(), clientIp, userAgent);
           throw new IllegalStateException("UNAVAILABLE ACCESS LINK");
        }
        // TODO 비동기 메시지 큐 도입하여 대체
        linkService.increaseLinkCount(link.getId());

        linkAnalyticsService.putAccessCount(link.getId(),
                new PutAccessLogDto(
                        ClientMapper.parseClientIp(request),
                        ClientMapper.parseLocale(request),
                        ClientMapper.parseUserAgent(request),
                        ClientMapper.parseReferer(request)
        ));
        response.sendRedirect(link.getRedirectionUrl());
    }

    @PutMapping("/link/update/{id}")
    public Link updateLink(@PathVariable Long id, @RequestBody String redirectionUrl) {
        Link link = linkService.updateRedirectionUrl(id, redirectionUrl);
        linkAnalyticsService.deleteAccessCountByLinkId(id);
        log.info("Updated link: {}", link);
        return link;
    }

    @DeleteMapping("/{id}")
    public void deleteLink(@PathVariable Long id) {
        linkAnalyticsService.deleteAccessCountByLinkId(id);
        linkService.deleteLinkById(id);
        log.info("Deleted link: {}", id);
    }
}
