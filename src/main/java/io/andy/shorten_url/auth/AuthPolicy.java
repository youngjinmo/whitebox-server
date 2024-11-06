package io.andy.shorten_url.auth;

public final class AuthPolicy {
    private AuthPolicy() {}
    // login
    public static final int LOGIN_SESSION_ACTIVE_TIME = 60 * 60 * 24;
    public static final String LOGIN_SESSION_KEY = "AUTH";

    // email
    public static final int EMAIL_AUTH_SESSION_ACTIVE_TIME = 60 * 15; // 15 분
    public static final String EMAIL_AUTH_SESSION_KEY = "EMAIL_AUTH";
    public static final int SECRET_CODE_LENGTH = 6;

    // reset password
    public static final int RESET_PASSWORD_LENGTH = 10;

    // auth token
    public static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15;       // 15 minutes
    public static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24; // 24 hours
}
