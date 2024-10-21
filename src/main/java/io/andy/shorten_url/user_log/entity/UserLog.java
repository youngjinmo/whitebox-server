package io.andy.shorten_url.user_log.entity;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.dto.AccessInfoDto;
import io.andy.shorten_url.user_log.dto.UpdateInfoDto;
import io.andy.shorten_url.user_log.dto.UpdatePrivacyInfoDto;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @ToString
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

    // JPA를 위한 디폴트 생성자
    public UserLog() {}

    // 사용자 접속 정보(SIGNUP/LOGIN/LOGOUT/DELETE)를 저장하는 UserLog 생성자
    public UserLog(AccessInfoDto userLogDto) {
        this.userId = userLogDto.userId();
        this.state = userLogDto.state();
        this.role = userLogDto.role();
        this.message = userLogDto.message();
        this.ipAddress = userLogDto.ipAddress();
        this.userAgent = userLogDto.userAgent();
        this.createdAt = LocalDateTime.now();
    }

    // 사용자 변경 정보(UPDATE)를 저장하는 UserLog 생성자
    public UserLog(UpdateInfoDto userLogDto) {
        this.userId = userLogDto.userId();
        this.state = userLogDto.state();
        this.role = userLogDto.role();
        this.message = userLogDto.message();
        this.preValue = userLogDto.preValue();
        this.postValue = userLogDto.postValue();
        this.createdAt = LocalDateTime.now();
    }

    // 민감 정보 변경시 저장하는 UserLog 생성자
    public UserLog(UpdatePrivacyInfoDto userLogDto) {
        this.userId = userLogDto.userId();
        this.state = userLogDto.state();
        this.role = userLogDto.role();
        this.message = userLogDto.message();
        this.createdAt = LocalDateTime.now();
    }
}
