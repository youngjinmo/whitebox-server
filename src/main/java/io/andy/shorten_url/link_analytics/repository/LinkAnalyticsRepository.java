package io.andy.shorten_url.link_analytics.repository;

import io.andy.shorten_url.common.CommonRepository;
import io.andy.shorten_url.link_analytics.entity.LinkAnalytics;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkAnalyticsRepository extends JpaRepository<LinkAnalytics, Long>, CommonRepository<LinkAnalytics> {
    Long countByLinkId(Long linkId);
    Page<LinkAnalytics> findByLinkId(Long linkId, Pageable pageable);
    void deleteByLinkId(Long linkId);
}
