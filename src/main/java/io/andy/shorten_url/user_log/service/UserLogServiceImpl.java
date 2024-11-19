package io.andy.shorten_url.user_log.service;

import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.dto.AccessUserInfoDto;
import io.andy.shorten_url.user_log.dto.UpdateUserInfoDto;
import io.andy.shorten_url.user_log.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.user_log.entity.UserLog;
import io.andy.shorten_url.user_log.repository.UserLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserLogServiceImpl implements UserLogService {
    private final UserLogRepository userLogRepository;

    @Override
    public void putUserAccessLog(AccessUserInfoDto userAccessDto) {
        userLogRepository.save(new UserLog(userAccessDto));
    }

    @Override
    public void putUpdateInfoLog(UpdateUserInfoDto updateUserInfoDto) {
        userLogRepository.save(new UserLog(updateUserInfoDto));
    }

    @Override
    public void putUpdateInfoLog(UpdatePrivacyInfoDto logDto) {
        userLogRepository.save(new UserLog(logDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLog> getUserLogsByUserId(Long userId) {
        return userLogRepository.findByUserId(userId).orElseThrow(() -> {
            log.debug("not found user logs by userId={}", userId);
            return new NotFoundException("NOT FOUND USER LOGS BY USER ID");
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLog> getUserLogsByMessage(UserLogMessage message) {
        return userLogRepository.findByMessage(message).orElseThrow(() -> {
            log.debug("not found user logs by message={}", message);
            return new NotFoundException("NOT FOUND USER LOGS BY MESSAGE");
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLog> findAllUserLogs() {
        return userLogRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserLog> findLatestUserLogs(int limit) {
        return userLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).orElseThrow(() -> new NotFoundException("NOT FOUND USER LOGS"));
    }
}
