package io.andy.shorten_url.link_analytics.service;

import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.link_analytics.dto.PutAccessLogDto;
import io.andy.shorten_url.link_analytics.repository.LinkAnalyticsRepository;
import io.andy.shorten_url.link_analytics.entity.LinkAnalytics;
import io.andy.shorten_url.util.ip.IpApiResponse;
import io.andy.shorten_url.util.ip.IpLocationUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkAnalyticsServiceImpl implements LinkAnalyticsService {
    private final LinkAnalyticsRepository linkAnalyticsRepository;
    private final IpLocationUtils ipLocationUtils;

    @Override
    public void putAccessCount(Long linkId, PutAccessLogDto accessLogDto) {
        try {
            IpApiResponse externalApiResponse = ipLocationUtils.getLocationByIp(accessLogDto.getIpAddress());
            if (Objects.isNull(externalApiResponse)) {
                accessLogDto.setLocation("unknown");
            }
            linkAnalyticsRepository.save(new LinkAnalytics(linkId, accessLogDto));
        } catch (Exception e) {
            log.error("failed to get location by ip, message={}", e.getMessage());
        }
    }

    @Override
    public Page<LinkAnalytics> findAllAccessCounts(Pageable pageable) {
        return linkAnalyticsRepository.findAll(pageable);
    }

    @Override
    public Page<LinkAnalytics> findAccessCountsByLinkId(Long linkId, Pageable pageable) {
        return linkAnalyticsRepository.findByLinkId(linkId, pageable);
    }

    @Override
    public Page<LinkAnalytics> findLatestLinkCountsWithinNdays(Long linkId, int days, Pageable pageable) {
        Page<LinkAnalytics> linkAnalytics = findAccessCountsByLinkId(linkId, pageable);
        List<LinkAnalytics> list = linkAnalytics.getContent()
                .stream()
                .filter(analytic -> analytic
                        .getCreatedAt()
                        .isAfter(LocalDateTime.now().minusDays(days)))
                .toList();
        return new PageImpl<>(list, pageable, linkAnalytics.getTotalElements());
    }

    @Override
    public void deleteAccessCountsByLinkId(Long linkId) {
        try {
            Long counts = linkAnalyticsRepository.countByLinkId(linkId);
            linkAnalyticsRepository.deleteByLinkId(linkId);
            log.info("deleted access counts={} by linkId={}", counts, linkId);
        } catch (Exception e) {
            log.error("failed to delete analytics by link id={}, message={}", linkId, e.getMessage());
            throw new InternalServerException();
        }
    }
}
