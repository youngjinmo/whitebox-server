package io.andy.shorten_url.link_counter.service;

import io.andy.shorten_url.link_counter.dto.PutAccessLogDto;
import io.andy.shorten_url.link_counter.entity.LinkCounter;

import java.util.List;

public interface LinkCounterService {
    void putAccessCount(PutAccessLogDto accessLogDto, Long linkId);
    List<LinkCounter> findAllAccessCounts();
    List<LinkCounter> findAccessCountsByLinkId(Long linkId);
    List<LinkCounter> findLatestLinkCountsWithinNdays(Long linkId, int days);
    void deleteAccessCountByLinkId(Long linkId);
}
