package io.andy.shorten_url.link_analytics.service;

import io.andy.shorten_url.link_analytics.dto.PutAccessLogDto;
import io.andy.shorten_url.link_analytics.entity.LinkAnalytics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LinkAnalyticsService {
    void putAccessCount(Long linkId, PutAccessLogDto putAccessLogDto);
    Page<LinkAnalytics> findAllAccessCounts(Pageable pageable);
    Page<LinkAnalytics> findAccessCountsByLinkId(Long linkId, Pageable pageable);
    Page<LinkAnalytics> findLatestLinkCountsWithinNdays(Long linkId, int days, Pageable pageable);
    void deleteAccessCountByLinkId(Long linkId);
}
