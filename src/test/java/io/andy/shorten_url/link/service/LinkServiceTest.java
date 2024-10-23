package io.andy.shorten_url.link.service;

import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.link.constant.LinkPolicy;
import io.andy.shorten_url.link.constant.LinkState;
import io.andy.shorten_url.link.dto.CreateLinkDto;
import io.andy.shorten_url.link.entity.Link;
import io.andy.shorten_url.link.repository.LinkRepository;
import io.andy.shorten_url.util.random.RandomStringGenerator;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class LinkServiceTest {
    @Mock private RandomStringGenerator randomUtility;
    @Mock private LinkRepository linkRepository;
    @InjectMocks private LinkServiceImpl linkService;

    @Test
    @DisplayName("링크 생성")
    void createLink() {
        // given
        Long userId = 1L;
        String redirectionUrl = "https://github.com/youngjinmo";
        String shortenUrlPath = "shorten_";
        CreateLinkDto dto = new CreateLinkDto(userId, redirectionUrl);
        Link expectedLink = new Link(userId, LinkState.PUBLIC, shortenUrlPath, redirectionUrl);

        // when
        when(randomUtility.generate(LinkPolicy.URL_PATH_LENGTH)).thenReturn(shortenUrlPath);
        when(linkRepository.findByUrlPath(shortenUrlPath)).thenReturn(Optional.empty());
        when(linkRepository.save(any(Link.class))).thenReturn(expectedLink);
        Link result = linkService.createLink(dto);

        // then
        assertEquals(userId, result.getUserId());
        assertNotNull(result.getUrlPath());
        assertEquals(LinkPolicy.URL_PATH_LENGTH, result.getUrlPath().length());
        assertEquals(redirectionUrl, result.getRedirectionUrl());
    }

    @Test
    @DisplayName("link id 기반으로 링크 조회")
    void findLinkById() {
        // given
        Long userId = 1L;
        String redirectionUrl = "https://github.com/youngjinmo";
        String shortenUrlPath = "shorten_";
        Link expectedLink = new Link(userId, LinkState.PUBLIC, shortenUrlPath, redirectionUrl);
        expectedLink.setId(1L);

        // when
        when(linkRepository.findById(expectedLink.getId())).thenReturn(Optional.of(expectedLink));
        Link result = linkService.findLinkById(expectedLink.getId());

        // then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(shortenUrlPath, result.getUrlPath());
        assertEquals(redirectionUrl, result.getRedirectionUrl());
    }

    @Test
    @DisplayName("link id 기반으로 링크 조회시 없으면 404 예외")
    void throwNotFoundFindWithoutLink() {
        // given
        Long userId = 1L;
        String redirectionUrl = "https://github.com/youngjinmo";
        String shortenUrlPath = "shorten_";
        Link expectedLink = new Link(userId, LinkState.PUBLIC, shortenUrlPath, redirectionUrl);

        // when
        when(linkRepository.findById(expectedLink.getId())).thenReturn(Optional.empty());
        NotFoundException exception = assertThrows(NotFoundException.class, () -> linkService.findLinkById(expectedLink.getId()));

        // then
        assertEquals("NOT FOUND LINK", exception.getMessage());
    }

    @Test
    @DisplayName("user id 기반으로 링크 조회")
    void findLinksByUserId() {
        // given
        Long userId = 1L;
        String redirectionUrl = "https://github.com/youngjinmo";
        List<Link> list = new ArrayList<>();
        list.add(new Link(userId, LinkState.PUBLIC, "shorten1", redirectionUrl));
        list.add(new Link(userId, LinkState.PUBLIC, "shorten2", redirectionUrl));

        // when
        when(linkRepository.findByUserId(userId)).thenReturn(list);
        List<Link> result = linkService.findLinksByUserId(userId);

        // then
        assertNotNull(result);
        assertEquals(list.size(), result.size());
        assertEquals(list.get(0).getUserId(), result.get(0).getUserId());
    }

    @Test
    @DisplayName("user id 기반으로 링크 조회시 없으면 빈 배열 반환")
    void findEmptyLinksByUserId() {
        // given
        Long userId = 1L;
        List<Link> list = new ArrayList<>();

        // when
        when(linkRepository.findByUserId(userId)).thenReturn(list);
        List<Link> result = linkService.findLinksByUserId(userId);

        // then
        assertNotNull(result);
        assertEquals(list.size(), result.size());
    }

    @ParameterizedTest
    @DisplayName("링크 유효성 검증")
    @ValueSource(booleans = {true, false})
    void isUniqueUrlPath(boolean expected) {
        // given
        String shortenUrlPath = "shorten_";
        Optional<Link> entityResult = Optional.empty();
        if(!expected) {
            Link link = new Link(1L, LinkState.PUBLIC, shortenUrlPath, "https://github.com/youngjinmo");
            entityResult = Optional.of(link);
        }

        // when
        when(linkRepository.findByUrlPath(shortenUrlPath)).thenReturn(entityResult);
        boolean isUnique = linkService.isUniqueUrlPath(shortenUrlPath);

        // then
        assertEquals(expected, isUnique);
    }

    @Test
    @DisplayName("url path 기반으로 Link 조회")
    void findLinkByUrlPath() {
        // given
        Long userId = 1L;
        String redirectionUrl = "https://github.com/youngjinmo";
        String shortenUrlPath = "shorten_";
        Link expectedLink = new Link(userId, LinkState.PUBLIC, shortenUrlPath, redirectionUrl);

        // when
        when(linkRepository.findByUrlPath(shortenUrlPath)).thenReturn(Optional.of(expectedLink));
        Link result = linkService.findLinkByUrlPath(shortenUrlPath);

        // then
        assertNotNull(result);
        assertEquals(redirectionUrl, result.getRedirectionUrl());
    }

    @Test
    @DisplayName("url path 기반으로 Link 조회했을때 없으면 404 예외 반환")
    void findEmptyLinkByUrlPath() {
        // given
        String shortenUrlPath = "shorten_";

        // when
        when(linkRepository.findByUrlPath(shortenUrlPath)).thenReturn(Optional.empty());
        NotFoundException exception = assertThrows(NotFoundException.class, () -> linkService.findLinkByUrlPath(shortenUrlPath));

        // then
        assertEquals("NOT FOUND LINK", exception.getMessage());
    }

    @Test
    @DisplayName("링크 상태 변경")
    public void updateLinkState() {
        // given
        Long userId = 1L;
        String redirectionUrl = "https://github.com/youngjinmo";
        String shortenUrlPath = "shorten_";
        Link expectedLink = new Link(userId, LinkState.PUBLIC, shortenUrlPath, redirectionUrl);

        // when
        when(linkRepository.findById(expectedLink.getId())).thenReturn(Optional.of(expectedLink));
        Link result = linkService.updateLinkState(expectedLink.getId(), LinkState.PRIVATE);

        // then
        assertEquals(LinkState.PRIVATE, result.getState());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    @DisplayName("링크 redirectionUrl 변경")
    public void updateRedirectionUrl() {
        // given
        Long userId = 1L;
        String originRedirectionUrl = "https://github.com/youngjinmo";
        String newRedirectUrl = "https://youngjinmo.com";
        String shortenUrlPath = "shorten_";
        Link expectedLink = new Link(userId, LinkState.PUBLIC, shortenUrlPath, originRedirectionUrl);

        // when
        when(linkRepository.findById(expectedLink.getId())).thenReturn(Optional.of(expectedLink));
        Link result = linkService.updateRedirectionUrl(expectedLink.getId(), newRedirectUrl);

        // then
        assertEquals(newRedirectUrl, result.getRedirectionUrl());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    @DisplayName("링크 삭제")
    public void deleteLink() {
        // given
        Long userId = 1L;
        String redirectionUrl = "https://github.com/youngjinmo";
        String shortenUrlPath = "shorten_";
        Link link = new Link(userId, LinkState.PUBLIC, shortenUrlPath, redirectionUrl);

        // when
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        linkService.deleteLinkById(link.getId());

        // then
        assertEquals(LinkState.DELETE, link.getState());
        assertNotNull(link.getDeletedAt());
    }

    @Test
    @DisplayName("링크 조회수 증가")
    public void increaseLinkCount() {
        // given
        Long userId = 1L;
        String redirectionUrl = "https://github.com/youngjinmo";
        String shortenUrlPath = "shorten_";
        Link link = new Link(userId, LinkState.PUBLIC, shortenUrlPath, redirectionUrl);
        link.setAccessCount(0L);

        // when
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        Long result = linkService.increaseLinkCount(link.getId());

        // then
        assertEquals(1, result);
    }

    @Test
    @DisplayName("비공개 링크에 접근시 예외 발생")
    public void throwBadRequestWithNonPublic() {
        // given
        Long userId = 1L;
        Long initCount = 0L;
        String redirectionUrl = "https://github.com/youngjinmo";
        String shortenUrlPath = "shorten_";
        Link link = new Link(userId, LinkState.PRIVATE, shortenUrlPath, redirectionUrl);
        link.setAccessCount(initCount);

        // when
        when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        BadRequestException exception = assertThrows(BadRequestException.class, () -> linkService.increaseLinkCount(link.getId()));

        // then
        assertEquals("FAILED TO ACCESS LINK", exception.getMessage());
        assertEquals(initCount, link.getAccessCount());
    }
}