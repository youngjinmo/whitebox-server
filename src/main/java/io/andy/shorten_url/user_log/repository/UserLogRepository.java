package io.andy.shorten_url.user_log.repository;

import io.andy.shorten_url.common.CommonRepository;
import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.entity.UserLog;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLogRepository extends JpaRepository<UserLog, Long>, CommonRepository<UserLog> {
    Optional<List<UserLog>> findByUserId(Long userId);
    Optional<List<UserLog>> findByMessage(UserLogMessage message);
    @Override @Profile("test") void deleteAll();
}
