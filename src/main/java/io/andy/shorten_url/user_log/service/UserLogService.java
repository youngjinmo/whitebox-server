package io.andy.shorten_url.user_log.service;

import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.dto.AccessUserInfoDto;
import io.andy.shorten_url.user_log.dto.UpdateUserInfoDto;
import io.andy.shorten_url.user_log.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.user_log.entity.UserLog;

import java.util.List;

public interface UserLogService {
    void putUserAccessLog(AccessUserInfoDto logDto);
    void putUpdateInfoLog(UpdateUserInfoDto logDto);
    void putUpdateInfoLog(UpdatePrivacyInfoDto logDto);
    List<UserLog> getUserLogsByUserId(Long userId);
    List<UserLog> getUserLogsByMessage(UserLogMessage message);
    List<UserLog> findAllUserLogs();
    List<UserLog> findLatestUserLogs(int limit);
}
