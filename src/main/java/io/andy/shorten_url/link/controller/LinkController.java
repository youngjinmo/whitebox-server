package io.andy.shorten_url.link.controller;

import io.andy.shorten_url.link.dto.CreateLinkDto;
import io.andy.shorten_url.link.entity.Link;
import io.andy.shorten_url.link.service.LinkService;

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

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
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

    @GetMapping("/{urlPath}")
    public void redirectUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String urlPath
    ) throws IOException {
        Link link = linkService.findLinkByUrlPath(urlPath);
        linkService.increaseLinkCount(link.getId());
        log.info("access link, ur path={}, redirectionUrl={}, ip={}, userAgent={}",
                link.getUrlPath(),
                link.getRedirectionUrl(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        );
        response.sendRedirect(link.getRedirectionUrl());
    }

    @PutMapping("/link/update/{id}")
    public Link updateLink(@PathVariable Long id, @RequestParam String redirectionUrl, @RequestParam Long userId) {
        Link link = linkService.updateRedirectionUrl(id, redirectionUrl);
        log.info("Updated link: {}", link);
        return link;
    }

    @DeleteMapping("/{id}")
    public void deleteLink(@PathVariable Long id) {
        linkService.deleteLinkById(id);
        log.info("Deleted link: {}", id);
    }
}
