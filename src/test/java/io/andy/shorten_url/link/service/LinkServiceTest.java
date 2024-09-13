package io.andy.shorten_url.link.service;

import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.link.constant.LinkPolicy;
import io.andy.shorten_url.link.constant.LinkState;
import io.andy.shorten_url.link.dto.CreateLinkDto;
import io.andy.shorten_url.link.entity.Link;
import io.andy.shorten_url.link.repository.LinkRepository;

import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class LinkServiceTest {
    @Autowired private LinkRepository linkRepository;
    @Autowired private LinkService linkService;

    private final Long mockUserId = 9999L;

    @BeforeEach
    public void flushBefore() {
        linkRepository.deleteAll();
    }

    @AfterEach
    public void flushAfter() {
        linkRepository.deleteAll();
    }

    @Test
    @DisplayName("링크 생성")
    void createLink() {
        String redirectionUrl = "http://localhost/9999";

        Link link = linkService.createLink(
                new CreateLinkDto(mockUserId, redirectionUrl));

        assertEquals(mockUserId, link.getUserId());
        assertNotNull(link.getUrlPath());
        assertEquals(LinkPolicy.URL_PATH_LENGTH, link.getUrlPath().length());
        assertEquals(redirectionUrl, link.getRedirectionUrl());
    }

    @Test
    @DisplayName("link id 기반으로 링크 조회")
    void findLinkById() {
        Link link = createDummyLink();

        Link result = linkService.findLinkById(link.getId());

        assertNotNull(result);
        assertEquals(mockUserId, result.getUserId());
    }

    @Test
    @DisplayName("user id 기반으로 링크 조회")
    void findLinksByUserId() {
        createDummyLinks(2);

        List<Link> result = linkService.findLinksByUserId(mockUserId);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("전체 링크 조회")
    void findAllLinks() {
        createDummyLinks(3);

        List<Link> result = linkService.findAllLinks();

        assertEquals(3, result.size());
        assertEquals(linkRepository.count(), result.size());
    }

    @Test
    @DisplayName("링크 유효성 검증")
    void isUniqueUrlPath() {
        String urlPath = "hello";
        createDummyLink(urlPath);

        boolean result1 = linkService.isUniqueUrlPath("hello");
        boolean result2 = linkService.isUniqueUrlPath("world");

        assertFalse(result1);
        assertTrue(result2);
    }

    @Test
    @DisplayName("url path 기반으로 Link 조회")
    void findLinkByUrlPath() {
        String urlPath = "hello";
        String redirectionUrl = "http://localhost/hello";
        createDummyLink(urlPath, redirectionUrl);

        Link result = linkService.findLinkByUrlPath(urlPath);
        assertEquals(redirectionUrl, result.getRedirectionUrl());
    }

    @Test
    @DisplayName("링크 상태 변경")
    public void updateLinkState() {
        Link link = createDummyLink();

        linkService.updateLinkState(link.getId(), LinkState.PRIVATE);
        Link result = linkRepository.findById(link.getId()).get();

        assertEquals(LinkState.PRIVATE, result.getState());
        assertNotNull(link.getUpdatedAt());
    }

    @Test
    @DisplayName("링크 redirectionUrl 변경")
    public void updateRedirectionUrl() {
        Link link = createDummyLink();
        String previousRedirectionUrl = link.getRedirectionUrl();

        Link updatedLink = linkService.updateRedirectionUrl(link.getId(), "http://hello.world");

        assertNotEquals(previousRedirectionUrl, updatedLink.getRedirectionUrl());
        assertNotNull(updatedLink.getUpdatedAt());
    }

    @Test
    @DisplayName("링크 삭제")
    public void deleteLink() {
        Link link = createDummyLink();

        linkService.deleteLinkById(link.getId());

        assertThatThrownBy(() -> linkService.findLinkById(link.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("링크 조회수 증가")
    public void increaseLinkCount() {
        Link link = createDummyLink();

        Long accessCount = linkService.increaseLinkCount(link.getId());

        assertEquals(1, accessCount);
    }

    private void createDummyLinks(int size) {
        for (int i = 0; i < size; i++) {
            String redirectionUrl = "localhost:8080/"+i;
            Link link = new Link(
                    mockUserId,
                    LinkState.PUBLIC,
                    "urlPath/"+i,
                    redirectionUrl
            );
            linkRepository.save(link);
        }
    }

    private Link createDummyLink(String... values) {
        String urlPath;         // values[0]
        String redirectionUrl;  // values[1]

        switch (values.length) {
            case 2 -> {
                urlPath = values[0];
                redirectionUrl = values[1];
            }
            case 1 -> {
                urlPath = values[0];
                redirectionUrl = "localhost:8080";
            }
            default -> {
                urlPath = "hello";
                redirectionUrl = "localhost:8080";
            }
        }

        return linkRepository.save(new Link(
                mockUserId,
                LinkState.PUBLIC,
                urlPath,
                redirectionUrl
        ));
    }
}