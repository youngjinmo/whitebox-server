package io.andy.shorten_url.auth;

public final class AuthPolicy {
    private AuthPolicy() {}
    // auth token
    public static final String AUTH_TOKEN_KEY_PREFIX = "auth:token";
    public static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15;       // 15 minutes
    public static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24; // 24 hours

    // email verification code
    public static final String EMAIL_AUTH_SESSION_KEY_PREFIX = "auth:email";
    public static final String RESET_PASSWORD_SESSION_KEY_PREFIX = "auth:reset-password";
    public static final int EMAIL_AUTH_SESSION_ACTIVE_TIME = 1000 * 60 * 15; // 15 분
    public static final int SECRET_CODE_LENGTH = 6;

    // reset password
    public static final int RESET_PASSWORD_LENGTH = 10;
}
