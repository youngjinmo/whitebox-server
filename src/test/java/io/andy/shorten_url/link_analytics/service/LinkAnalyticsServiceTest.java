package io.andy.shorten_url.link_analytics.service;

import io.andy.shorten_url.link.constant.LinkState;
import io.andy.shorten_url.link.entity.Link;
import io.andy.shorten_url.link_analytics.dto.PutAccessLogDto;
import io.andy.shorten_url.link_analytics.entity.LinkAnalytics;
import io.andy.shorten_url.link_analytics.repository.LinkAnalyticsRepository;
import io.andy.shorten_url.util.ip.IpApiResponse;
import io.andy.shorten_url.util.ip.IpLocationUtils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class LinkAnalyticsServiceTest {
    @Mock private LinkAnalyticsRepository repository;
    @Mock private IpLocationUtils ipLocationUtils;
    @InjectMocks private LinkAnalyticsServiceImpl linkAnalyticsService;

    @Test
    @DisplayName("link counter 저장")
    void putAccessCount() {
        // given
        String location = "KR";
        Link link = new Link(1L, LinkState.PUBLIC, "shorten_", "https://github.com");
        PutAccessLogDto dto = new PutAccessLogDto("127.0.0.1", location, "mac safari", "www.google.com");
        LinkAnalytics analytics = new LinkAnalytics(link.getId(), dto);

        IpApiResponse apiResponse = IpApiResponse.builder()
                .country(location)
                .build();

        // when
        when(ipLocationUtils.getLocationByIp(dto.getIpAddress())).thenReturn(apiResponse);
        when(repository.save(analytics)).thenReturn(analytics);
        linkAnalyticsService.putAccessCount(link.getId(), dto);

        // then
        verify(repository, times(1)).save(any(LinkAnalytics.class));
    }

    @ParameterizedTest
    @DisplayName("최근 3일 이내 생성된 link counter 조회")
    @ValueSource(ints = 3)
    void findLatestLinkCountsWithinNdays(int days) {
        // given
        Long linkId = 1L;
        LocalDateTime now = LocalDateTime.now();
        PutAccessLogDto dto = new PutAccessLogDto("127.0.0.1", "KO", "mac safari", "www.google.com");

        LinkAnalytics analytic1 = new LinkAnalytics(linkId, dto);
        analytic1.setCreatedAt(now.minusDays(5));
        LinkAnalytics analytic2 = new LinkAnalytics(linkId, dto);
        analytic2.setCreatedAt(now.minusDays(3));
        LinkAnalytics analytic3 = new LinkAnalytics(linkId, dto);
        analytic3.setCreatedAt(now.minusDays(2));

        List<LinkAnalytics> list = Arrays.asList(analytic1, analytic2, analytic3);
        Pageable pageable = PageRequest.of(0, 10);
        Page<LinkAnalytics> page = new PageImpl<>(list, pageable, list.size());

        // when
        when(repository.findByLinkId(linkId, pageable)).thenReturn(page);
        Page<LinkAnalytics> result = linkAnalyticsService.findLatestLinkCountsWithinNdays(linkId, days, pageable);

        // then
        assertEquals(1, result.stream().count());
        assertEquals(analytic3, result.getContent().get(0));
    }

    @Test
    @DisplayName("Link id 기반 link counter 삭제")
    void deleteAccessCountByLinkId() {
        // given
        Long linkId = 999L;

        // when
        when(repository.countsByLinkId(linkId)).thenReturn(100L);
        doNothing().when(repository).deleteByLinkId(linkId);
        linkAnalyticsService.deleteAccessCountByLinkId(linkId);

        // then
        verify(repository, times(1)).deleteByLinkId(linkId);
    }
}