CREATE TABLE user
(
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE COMMENT '사용자 이메일',
    password VARCHAR(255) COMMENT '비밀번호',
    state VARCHAR(255) NOT NULL COMMENT '회원 상태',
    role VARCHAR(255) NOT NULL COMMENT '역할/권한',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '회원 가입일',
    last_login_at DATETIME COMMENT '마지막 접속일',
    updated_at DATETIME comment '정보 수정일',
    withdrawn_at DATETIME COMMENT '계정 탈퇴일',
    deleted_at DATETIME COMMENT '계정 삭제일'
);

CREATE TABLE user_log
(
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL COMMENT '회원 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그 생성일',
    state VARCHAR(255) NOT NULL COMMENT '로깅 시점 회원 상태',
    role VARCHAR(255) NOT NULL COMMENT '로깅 시점 역할/권한',
    message VARCHAR(255) COMMENT '로그 메시지',
    pre_value VARCHAR(255) COMMENT '수정 전 값',
    post_value VARCHAR(255) COMMENT '수정 후 값',
    ip_address VARCHAR(255) COMMENT 'ip',
    user_agent VARCHAR(255) COMMENT 'user-agent',
    CONSTRAINT fk_user_log_user_id FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE link
(
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    state VARCHAR(255) NOT NULL COMMENT '링크 상태',
    user_id INT UNSIGNED NOT NULL COMMENT '회원 ID',
    url_path VARCHAR(255) NOT NULL UNIQUE COMMENT '생성 URL path',
    redirection_url VARCHAR(255) NOT NULL COMMENT '리다이렉션 URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '링크 생성일',
    updated_at DATETIME COMMENT '링크 수정일',
    deleted_at DATETIME COMMENT '링크 삭제일',
    access_count INT UNSIGNED DEFAULT 0 COMMENT '접속 수',
    CONSTRAINT fk_link_user_id FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE link_log
(
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '로그 생성일',
    link_id INT UNSIGNED COMMENT '링크 ID',
    user_id INT UNSIGNED NOT NULL COMMENT '생성 회원 ID',
    link_state VARCHAR(255) NOT NULL COMMENT '링크 상태',
    url_path VARCHAR(255) NOT NULL COMMENT '생성 URL path',
    redirection_url VARCHAR(255) NOT NULL COMMENT '리다이렉션 URL',
    message VARCHAR(255) COMMENT '로그 메시지',
    pre_value VARCHAR(255) COMMENT '수정 전 값',
    post_value VARCHAR(255) COMMENT '수정 후 값',
    CONSTRAINT fk_link_log_link_id FOREIGN KEY (link_id) REFERENCES link (id)
);

CREATE TABLE link_analytics
(
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '접속 일',
    link_id INT UNSIGNED NOT NULL COMMENT '링크 ID',
    ip_address VARCHAR(255) COMMENT '접속 IP',
    user_agent VARCHAR(255) COMMENT '접속 에이전트',
    location VARCHAR(255) COMMENT '접속 지역',
    referer VARCHAR(255) COMMENT 'referer 링크',
    CONSTRAINT fk_link_analytics_link_id FOREIGN KEY (link_id) REFERENCES link (id)
);
