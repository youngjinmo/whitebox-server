package io.andy.shorten_url.link.controller;

import io.andy.shorten_url.link.dto.CreateLinkDto;
import io.andy.shorten_url.link.entity.Link;
import io.andy.shorten_url.link.service.LinkService;

import io.andy.shorten_url.link_counter.dto.PutAccessLogDto;
import io.andy.shorten_url.link_counter.service.LinkCounterService;
import io.andy.shorten_url.session.SessionService;
import io.andy.shorten_url.util.ClientMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
public class LinkController {
    @Autowired private final LinkService linkService;
    @Autowired private final LinkCounterService linkCounterService;
    @Autowired private final SessionService sessionService;

    public LinkController(
            LinkService linkService,
            LinkCounterService linkCounterService,
            SessionService sessionService
    ) {
        this.linkService = linkService;
        this.linkCounterService = linkCounterService;
        this.sessionService = sessionService;
    }

    @PostMapping("/link/create")
    public Link createLink(@RequestParam String redirectionUrl, @RequestParam Long userId) {
        Link link = linkService.createLink(new CreateLinkDto(userId, redirectionUrl));
        log.info("Created link: {}", link);
        return link;
    }

    @GetMapping("/link/all")
    public List<Link> findAllLinks() {
        return linkService.findAllLinks();
    }

    @GetMapping(value = {"/{urlPath}", "/{urlPath}/"})
    public void redirectUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String urlPath
    ) throws IOException {
        Link link = linkService.findLinkByUrlPath(urlPath);
        linkService.increaseLinkCount(link.getId());

        linkCounterService.putAccessCount(new PutAccessLogDto(
                ClientMapper.parseClientIp(request),
                ClientMapper.parseLocale(request),
                ClientMapper.parseUserAgent(request),
                ClientMapper.parseReferer(request)
        ), link.getId());
        response.sendRedirect(link.getRedirectionUrl());
    }

    @PutMapping("/link/update/{id}")
    public Link updateLink(@PathVariable Long id, @RequestParam String redirectionUrl) {
        Link link = linkService.updateRedirectionUrl(id, redirectionUrl);
        linkCounterService.deleteAccessCountByLinkId(id);
        log.info("Updated link: {}", link);
        return link;
    }

    @DeleteMapping("/{id}")
    public void deleteLink(@PathVariable Long id) {
        linkCounterService.deleteAccessCountByLinkId(id);
        linkService.deleteLinkById(id);
        log.info("Deleted link: {}", id);
    }
}
