package io.andy.shorten_url.link_counter.repository;

import io.andy.shorten_url.common.CommonRepository;
import io.andy.shorten_url.link_counter.entity.LinkCounter;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkCounterRepository extends JpaRepository<LinkCounter, Long>, CommonRepository<LinkCounter> {
    List<LinkCounter> findByLinkId(Long linkId);
    void deleteByLinkId(Long linkId);
    @Override @Profile("test") void deleteAll();
}
