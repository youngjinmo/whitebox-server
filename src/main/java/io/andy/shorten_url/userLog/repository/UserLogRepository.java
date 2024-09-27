package io.andy.shorten_url.userLog.repository;

import io.andy.shorten_url.userLog.constant.UserLogMessage;
import io.andy.shorten_url.userLog.entity.UserLog;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long> {
    Optional<List<UserLog>> findByUserId(Long userId);
    Optional<List<UserLog>> findByMessage(UserLogMessage message);
    Optional<List<UserLog>> findByOrderByCreatedAtDesc(Pageable pageable);
}
