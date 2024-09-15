package io.andy.shorten_url.user_log.service;

import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.dto.AccessInfoDto;
import io.andy.shorten_url.user_log.dto.UpdateInfoDto;
import io.andy.shorten_url.user_log.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.user_log.entity.UserLog;
import io.andy.shorten_url.user_log.repository.UserLogRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class UserLogServiceImpl implements UserLogService {

    @Autowired private final UserLogRepository userLogRepository;

    public UserLogServiceImpl(UserLogRepository userLogRepository) {
        this.userLogRepository = userLogRepository;
    }

    @Override
    public void putUserAccessLog(AccessInfoDto userAccessDto) {
        userLogRepository.save(new UserLog(userAccessDto));
    }

    @Override
    public void putUpdateInfoLog(UpdateInfoDto updateInfoDto) {
        userLogRepository.save(new UserLog(updateInfoDto));
    }

    @Override
    public void putUpdateInfoLog(UpdatePrivacyInfoDto logDto) {
        userLogRepository.save(new UserLog(logDto));
    }

    @Override
    public List<UserLog> getUserLogsByUserId(Long userId) {
        return userLogRepository.findByUserId(userId).orElseThrow(() -> {
            log.debug("not found user logs by userId={}", userId);
            return new NotFoundException("NOT FOUND USER LOGS BY USER ID");
        });
    }

    @Override
    public List<UserLog> getUserLogsByMessage(UserLogMessage message) {
        return userLogRepository.findByMessage(message).orElseThrow(() -> {
            log.debug("not found user logs by message={}", message);
            return new NotFoundException("NOT FOUND USER LOGS BY MESSAGE");
        });
    }

    @Override
    public List<UserLog> findAllUserLogs() {
        return userLogRepository.findAll();
    }

    @Override
    public List<UserLog> findLatestUserLogs(int limit) {
        return userLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).orElseThrow(() -> new NotFoundException("NOT FOUND USER LOGS"));
    }
}
