package io.andy.shorten_url.userLog.entity;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.userLog.constant.UserLogMessage;
import io.andy.shorten_url.userLog.dto.AccessInfoDto;
import io.andy.shorten_url.userLog.dto.UpdateInfoDto;
import io.andy.shorten_url.userLog.dto.UpdatePrivacyInfoDto;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class UserLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // to be update db schema
    private Long userId;
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private UserState state;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Enumerated(EnumType.STRING)
    private UserLogMessage message;
    private String preValue;
    private String postValue;
    private String ipAddress;
    private String userAgent;

    public UserLog() {}

    // 사용자 접속 정보(SIGNUP/LOGIN/LOGOUT/DELETE)를 저장하는 UserLog 생성자
    public UserLog(AccessInfoDto userLogDto) {
        this.userId = userLogDto.userId();
        this.state = userLogDto.state();
        this.role = userLogDto.role();
        this.message = userLogDto.message();
        this.ipAddress = userLogDto.ipAddress();
        this.userAgent = userLogDto.userAgent();
    }

    // 사용자 변경 정보(UPDATE)를 저장하는 UserLog 생성자
    public UserLog(UpdateInfoDto userLogDto) {
        this.userId = userLogDto.userId();
        this.state = userLogDto.state();
        this.role = userLogDto.role();
        this.message = userLogDto.message();
        this.preValue = userLogDto.preValue();
        this.postValue = userLogDto.postValue();
    }

    // 민감 정보 변경시 저장하는 UserLog 생성자
    public UserLog(UpdatePrivacyInfoDto userLogDto) {
        this.userId = userLogDto.userId();
        this.state = userLogDto.state();
        this.role = userLogDto.role();
        this.message = userLogDto.message();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
