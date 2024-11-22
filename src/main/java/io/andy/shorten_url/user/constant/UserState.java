package io.andy.shorten_url.user.constant;

public enum UserState {
    NEW,  // 신규 가입한 회원 상태 (아직 최초 로그인을 하지않음)
    NORMAL, // 정상 회원 상태
    WITHDRAWN, // 탈퇴 중 상태
    DELETED // 삭제된 회원 상태
}
