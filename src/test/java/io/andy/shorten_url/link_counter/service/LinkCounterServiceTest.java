package io.andy.shorten_url.link_counter.service;

import io.andy.shorten_url.link_counter.dto.PutAccessLogDto;
import io.andy.shorten_url.link_counter.entity.LinkCounter;
import io.andy.shorten_url.link_counter.repository.LinkCounterRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class LinkCounterServiceTest {
    @Autowired private LinkCounterRepository linkCounterRepository;
    @Autowired private LinkCounterService linkCounterService;

    @BeforeEach
    public void initiate() {
        linkCounterRepository.deleteAll();
    }

    @AfterEach
    public void cleanup() {
        linkCounterRepository.deleteAll();
    }

    @Test
    @DisplayName("link counter 저장")
    void putAccessCount() {
        Long linkId = 999L;

        linkCounterService.putAccessCount(createDummyDto(), linkId);

        List<LinkCounter> result = linkCounterRepository.findByLinkId(linkId);
        assertEquals(1, result.size());
        assertEquals(linkId, result.get(0).getLinkId());
    }

    @Test
    @DisplayName("전체 link counter 조회")
    void findAllAccessCounts() {
        Long linkId = 999L;
        for (int i = 0; i < 5; i++) {
            linkCounterRepository.save(
                    new LinkCounter(linkId, createDummyDto()));
        }

        List<LinkCounter> result = linkCounterService.findAllAccessCounts();

        assertEquals(5, result.size());
        assertEquals(linkId, result.get(1).getLinkId());
    }

    @Test
    @DisplayName("Link id 기반 link counter 조회")
    void findAccessCountsByLinkId() {
        Long linkId = 999L;
        for (int i = 0; i < 5; i++) {
            linkCounterRepository.save(
                    new LinkCounter(linkId, createDummyDto()));
        }
        linkCounterRepository.save(new LinkCounter(1L, createDummyDto()));

        List<LinkCounter> result = linkCounterService.findAccessCountsByLinkId(linkId);

        assertEquals(5, result.size());
        assertEquals(linkId, result.get(1).getLinkId());
    }

    @Test
    @DisplayName("최근 n일 이내 생성된 link counter 조회")
    void findLatestLinkCountsWithinNdays() {
        Long linkId = 999L;
        for (int i = 0; i < 10; i++) {
            LinkCounter linkCounter = new LinkCounter(linkId, createDummyDto());
            linkCounter.setCreatedAt(LocalDateTime.now().minusDays(i));
            linkCounterRepository.save(linkCounter);
        }

        List<LinkCounter> result =
                linkCounterService.findLatestLinkCountsWithinNdays(linkId, 3);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("Link id 기반 link counter 삭제")
    void deleteAccessCountByLinkId() {
        Long linkId = 999L;
        LinkCounter linkCounter = linkCounterRepository.save(
                new LinkCounter(linkId, createDummyDto()));

        linkCounterService.deleteAccessCountByLinkId(linkId);

        assertTrue(linkCounterRepository.findById(linkCounter.getId()).isEmpty());
    }

    private PutAccessLogDto createDummyDto() {
        String ipAddress = "127.0.0.1";
        String location = "US";
        String userAgent = "chrome";
        String referer = "https://github.com";
        return new PutAccessLogDto(ipAddress, location, userAgent, referer);
    }
}