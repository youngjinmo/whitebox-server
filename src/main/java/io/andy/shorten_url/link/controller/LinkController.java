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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@RestController
public class LinkController {
    private final LinkService linkService;
    private final LinkAnalyticsService linkAnalyticsService;

    @PostMapping("/link/create")
    public ResponseEntity<Link> createLink(@RequestBody CreateLinkDto createLinkDto) {
        Link link = linkService.createLink(createLinkDto);
        log.info("created link: {}", link);
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    @GetMapping("/link/all")
    public ResponseEntity<Map<String, Object>> findAllLinks(Pageable pageable) {
        List<Link> links = linkService.findAllLinks(pageable);
        Map<String, Object> response = new HashMap<>();
        response.put("links", links);
        response.put("currentPage", pageable.getPageNumber());
        response.put("totalItems", links.size());
        response.put("pageSize", pageable.getPageSize());
        return ResponseEntity.ok(response);
    }

    @Transactional
    @GetMapping(value = {"/{urlPath}", "/{urlPath}/"})
    public ResponseEntity<Void> redirectUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String urlPath
    ) {
        Link link = linkService.findLinkByUrlPath(urlPath);
        if (!link.getState().equals(LinkState.PUBLIC)) {
            String clientIp = ClientMapper.parseClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            log.debug("접근 불가능한 링크에 대한 접근 시도입니다. link_id={}, link_state={}, ip={}, userAgent={}", link.getId(), link.getState(), clientIp, userAgent);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNAVAILABLE ACCESS LINK");
        }
        // TODO 비동기 메시지 큐 도입하여 대체
        try {
            linkService.increaseLinkCount(link.getId());
            linkAnalyticsService.putAccessCount(link.getId(),
                PutAccessLogDto.builder()
                        .ipAddress(ClientMapper.parseClientIp(request))
                        .userAgent(ClientMapper.parseUserAgent(request))
                        .referer(ClientMapper.parseReferer(request))
                        .build());
            response.sendRedirect(link.getRedirectionUrl());
            return ResponseEntity.ok().build();
        } catch (IOException | ResponseStatusException e) {
            log.error("[VERY IMPORTANT] failed to redirect url path={}", urlPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FAILED REDIRECTION BY SERVER ERROR");
        }
    }

    @Transactional
    @PutMapping("/link/update/{id}")
    public ResponseEntity<Link> updateLink(@PathVariable Long id, @RequestBody String redirectionUrl) {
        Link link = linkService.updateRedirectionUrl(id, redirectionUrl);
        linkAnalyticsService.deleteAccessCountsByLinkId(id);
        log.info("Updated link id={}, and delete all access counts.", id);
        return ResponseEntity.ok(link);
    }

    @Transactional
    @DeleteMapping("/link/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable Long id) {
        try {
            linkAnalyticsService.deleteAccessCountsByLinkId(id);
            linkService.deleteLinkById(id);
            log.info("Deleted link: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("failed to delete link id={}", id);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FAILED DELETE LINK");
        }
    }
}
