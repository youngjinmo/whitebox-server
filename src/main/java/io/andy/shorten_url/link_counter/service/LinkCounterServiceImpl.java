package io.andy.shorten_url.link_counter.service;

import io.andy.shorten_url.link_counter.dto.PutAccessLogDto;
import io.andy.shorten_url.link_counter.repository.LinkCounterRepository;
import io.andy.shorten_url.link_counter.entity.LinkCounter;
import io.andy.shorten_url.util.ip.IpApiResponse;
import io.andy.shorten_url.util.ip.IpLocationUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LinkCounterServiceImpl implements LinkCounterService {

    @Autowired private final LinkCounterRepository repository;
    @Autowired private final IpLocationUtils ipLocationUtils;

    public LinkCounterServiceImpl(LinkCounterRepository repository, IpLocationUtils ipLocationUtils) {
        this.repository = repository;
        this.ipLocationUtils = ipLocationUtils;
    }

    @Override
    public void putAccessCount(PutAccessLogDto accessLogDto, Long linkId) {
        try {
            IpApiResponse externalApiResponse = ipLocationUtils.getLocationByIp(accessLogDto.getIpAddress());
            if (externalApiResponse.country() != null) {
                accessLogDto.setLocation(externalApiResponse.country());
            }
            repository.save(new LinkCounter(linkId, accessLogDto));
        } catch (Exception e) {
            log.error("failed to get location by ip, message={}", e.getMessage());
        }
    }

    @Override
    public List<LinkCounter> findAllAccessCounts() {
        return repository.findAll();
    }

    @Override
    public List<LinkCounter> findAccessCountsByLinkId(Long linkId) {
        return repository.findByLinkId(linkId);
    }

    @Override
    public List<LinkCounter> findLatestLinkCountsWithinNdays(Long linkId, int days) {
        List<LinkCounter> linkCounters = this.findAccessCountsByLinkId(linkId);
        return linkCounters
                .stream()
                .filter(linkCounter -> linkCounter
                        .getCreatedAt()
                        .isAfter(LocalDateTime.now().minusDays(days)))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAccessCountByLinkId(Long linkId) {
        int counts = this.findAccessCountsByLinkId(linkId).size();
        repository.deleteByLinkId(linkId);
        log.info("deleted access counts={} by linkId={}", counts, linkId);
    }
}
