package io.andy.shorten_url.link.service;

import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.link.constant.LinkPolicy;
import io.andy.shorten_url.link.constant.LinkState;
import io.andy.shorten_url.link.dto.CreateFreeLinkDto;
import io.andy.shorten_url.link.dto.CreateLinkDto;
import io.andy.shorten_url.link.entity.Link;
import io.andy.shorten_url.link.repository.LinkRepository;
import io.andy.shorten_url.util.encrypt.EncodeUtil;
import io.andy.shorten_url.util.random.RandomUtility;
import io.andy.shorten_url.util.validation.Validator;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class LinkServiceImpl implements LinkService {
    private final LinkRepository linkRepository;
    private final RandomUtility randomUtility;

    @Autowired
    public LinkServiceImpl(
            LinkRepository linkRepository,
            @Qualifier("RandomStringGenerator") RandomUtility randomUtility
    ) {
        this.linkRepository = linkRepository;
        this.randomUtility = randomUtility;
    }

    @Override
    public Link createFreeLink(CreateFreeLinkDto linkDto) {
        if (!Validator.validateUrl(linkDto.redirectionUrl())) {
            throw new IllegalArgumentException("Invalid redirection URL");
        }

        // check ip in db(mysql/redis) if has created link

        // check duplicate url
        String shortenUrlPath = generateRandomUrlPath();

        // create link & save link in db
        Link link = linkRepository.save(new Link(
                LinkState.PUBLIC,
                shortenUrlPath,
                linkDto.redirectionUrl()
        ));

        // put log
        log.info("created link by free, linkId={}, ip={}", link, linkDto.ipAddress());

        return link;
    }

    @Override
    public Link createLink(CreateLinkDto linkDto) {
        if (!Validator.validateUrl(linkDto.redirectionUrl())) {
            throw new IllegalArgumentException("Invalid redirection URL");
        }

        // TODO 링크가 많아졌을 경우 무한루프 발생할 가능성 존재, 방어코드 필요
        String shortenUrlPath = generateRandomUrlPath();
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
        return linkRepository.findByUserId(userId);
    }

    @Override
    public List<Link> findAllLinks(Pageable pageable) {
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

        link.setState(state);
        link.setUpdatedAt(LocalDateTime.now());

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

        link.setState(LinkState.DELETE);
        link.setDeletedAt(LocalDateTime.now());
        link.setRedirectionUrl(EncodeUtil.encrypt(link.getRedirectionUrl()));

        log.info("deleted link={}", link);
    }

    // TODO 추후 비동기 메시지 큐 도입으로 대체
    @Override
    public long increaseLinkCount(Long id) {
        Link link = findLinkById(id);
        if (link.getState() == LinkState.PUBLIC) {
            long accessCount = link.getAccessCount() + 1L;
            link.setAccessCount(accessCount);
            return accessCount;
        } else {
            log.debug("공개된 링크가 아니기에 접근할 수 없습니다. link id={}, state={}", id, link.getState());
            throw new BadRequestException("FAILED TO ACCESS LINK");
        }
    }

    /**
     * generate random url path after check duplication
     * @return UrlPath
     */
    private String generateRandomUrlPath() {
        String shortenUrlPath = "";
        do {
            shortenUrlPath = randomUtility.generate(LinkPolicy.URL_PATH_LENGTH);
        } while (!isUniqueUrlPath(shortenUrlPath));
        return shortenUrlPath;
    }
}

