package io.andy.shorten_url.link.service;

import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.link.constant.LinkPolicy;
import io.andy.shorten_url.link.constant.LinkState;
import io.andy.shorten_url.link.dto.CreateLinkDto;
import io.andy.shorten_url.link.entity.Link;
import io.andy.shorten_url.link.repository.LinkRepository;
import io.andy.shorten_url.util.random.RandomUtility;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Transactional
@Service
public class LinkServiceImpl implements LinkService {

    @Autowired private LinkRepository linkRepository;
    private final RandomUtility randomUtility;

    public LinkServiceImpl(LinkRepository linkRepository, RandomUtility randomUtility) {
        this.linkRepository = linkRepository;
        this.randomUtility = randomUtility;
    }

    @Override
    public Link createLink(CreateLinkDto linkDto) {
        String shortenUrlPath = randomUtility.generate(LinkPolicy.URL_PATH_LENGTH);
        try {
            Link link = linkRepository.save(new Link(
                    linkDto.userId(),
                    LinkState.PUBLIC,
                    shortenUrlPath,
                    linkDto.redirectionUrl()
            ));
            log.info("created link={}", link);

            return link;
        } catch (Exception e) {
            log.error("failed to create link, userId={}, urlPath={}, redirectionUrl={}. error message={}",
                    linkDto.userId(), shortenUrlPath, linkDto.redirectionUrl(), e.getMessage());
            throw new InternalServerException("FAILED TO CREATE LINK");
        }
    }

    @Override
    public Link findLinkById(Long id) {
        Optional<Link> link = linkRepository.findById(id);
        if (link.isPresent()) {
            return link.get();
        }
        throw new NotFoundException("NOT FOUND LINK");
    }

    @Override
    public List<Link> findLinksByUserId(Long userId) {
        Optional<List<Link>> links = linkRepository.findByUserId(userId);
        if (links.isPresent()) {
            return links.get();
        }
        throw new NotFoundException("NOT FOUND LINK");
    }

    @Override
    public List<Link> findAllLinks() {
        return linkRepository.findAll();
    }

    @Override
    public boolean isUniqueUrlPath(String urlPath) {
        Optional<Link> link = linkRepository.findByUrlPath(urlPath);
        return link.isEmpty();
    }

    @Override
    public Link findLinkByUrlPath(String urlPath) {
        Optional<Link> link = linkRepository.findByUrlPath(urlPath);
        if (link.isPresent()) {
            return link.get();
        }
        throw new NotFoundException("NOT FOUND LINK");
    }

    @Override
    public Link updateLinkState(Long id, LinkState state) {
        Link link = findLinkById(id);
        LinkState previousState = link.getState();

        try {
            link.setState(state);
            link.setUpdatedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("failed to update link state, linkId={}, message={}", id, e.getMessage());
            throw new InternalServerException("FAILED TO UPDATE LINK");
        }

        log.info("updated link state to {} from {}", state, previousState);
        return link;
    }

    @Override
    public Link updateRedirectionUrl(Long id, String redirectionUrl) {
        Link link = findLinkById(id);
        String previousRedirectionUrl = link.getRedirectionUrl();

        link.setRedirectionUrl(redirectionUrl);
        link.setUpdatedAt(LocalDateTime.now());

        log.info("updated redirection url to {} from {}", redirectionUrl, previousRedirectionUrl);
        return link;
    }

    @Override
    public void deleteLinkById(Long id) {
        Link link = findLinkById(id);

        linkRepository.delete(link);

        log.info("deleted link={}", link);
    }

    @Override
    public long increaseLinkCount(Long id) {
        Link link = findLinkById(id);
        try {
            long accessCount = link.getAccessCount() + 1;
            link.setAccessCount(accessCount);
            return accessCount;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        log.error("failed to increase link count");
        return 0;
    }
}

