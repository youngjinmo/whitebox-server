package io.andy.shorten_url.common;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CommonRepository<Entity> {
    Optional<List<Entity>> findAllByOrderByCreatedAtAsc(Pageable pageable);
    Optional<List<Entity>> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
