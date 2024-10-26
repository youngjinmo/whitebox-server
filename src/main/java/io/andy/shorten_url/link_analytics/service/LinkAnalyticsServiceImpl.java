package io.andy.shorten_url.link_analytics.service;

import io.andy.shorten_url.link_analytics.dto.PutAccessLogDto;
import io.andy.shorten_url.link_analytics.repository.LinkAnalyticsRepository;
import io.andy.shorten_url.link_analytics.entity.LinkAnalytics;
import io.andy.shorten_url.util.ip.IpApiResponse;
import io.andy.shorten_url.util.ip.IpLocationUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class LinkAnalyticsServiceImpl implements LinkAnalyticsService {
    private final LinkAnalyticsRepository repository;
    private final IpLocationUtils ipLocationUtils;

    @Autowired
    public LinkAnalyticsServiceImpl(LinkAnalyticsRepository repository, IpLocationUtils ipLocationUtils) {
        this.repository = repository;
        this.ipLocationUtils = ipLocationUtils;
    }

    @Override
    public void putAccessCount(Long linkId, PutAccessLogDto accessLogDto) {
        try {
            IpApiResponse externalApiResponse = ipLocationUtils.getLocationByIp(accessLogDto.getIpAddress());
            if (externalApiResponse.country() != null) {
                accessLogDto.setLocation(externalApiResponse.country());
            }
            repository.save(new LinkAnalytics(linkId, accessLogDto));
        } catch (Exception e) {
            log.error("failed to get location by ip, message={}", e.getMessage());
        }
    }

    @Override
    public Page<LinkAnalytics> findAllAccessCounts(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Page<LinkAnalytics> findAccessCountsByLinkId(Long linkId, Pageable pageable) {
        return repository.findByLinkId(linkId, pageable);
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

    @Transactional
    @Override
    public void deleteAccessCountByLinkId(Long linkId) {
        Long counts = repository.countsByLinkId(linkId);
        repository.deleteByLinkId(linkId);
        log.info("deleted access counts={} by linkId={}", counts, linkId);
    }
}
