package io.andy.shorten_url.link.repository;

import io.andy.shorten_url.common.CommonRepository;
import io.andy.shorten_url.link.entity.Link;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long>, CommonRepository<Link> {
    List<Link> findByUserId(Long userId);
    Optional<Link> findByUrlPath(String urlPath);
}
