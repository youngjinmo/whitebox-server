package io.andy.shorten_url.userLog.service;

import io.andy.shorten_url.userLog.constant.UserLogMessage;
import io.andy.shorten_url.userLog.dto.AccessInfoDto;
import io.andy.shorten_url.userLog.dto.UpdateInfoDto;
import io.andy.shorten_url.userLog.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.userLog.entity.UserLog;

import java.util.List;

public interface UserLogService {
    void putUserAccessLog(AccessInfoDto logDto);
    void putUpdateInfoLog(UpdateInfoDto logDto);
    void putUpdateInfoLog(UpdatePrivacyInfoDto logDto);
    List<UserLog> getUserLogsByUserId(Long userId);
    List<UserLog> getUserLogsByMessage(UserLogMessage message);
    List<UserLog> findAllUserLogs();
    List<UserLog> findLatestUserLogs(int limit);
}
