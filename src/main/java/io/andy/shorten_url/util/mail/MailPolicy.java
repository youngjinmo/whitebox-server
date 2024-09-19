package io.andy.shorten_url.util.mail;

public final class MailPolicy {
    public static final int EMAIL_AUTH_SESSION_ACTIVE_TIME = 60 * 15; // 15 분
    public static final String EMAIL_AUTH_SESSION_KEY = "EMAIL_AUTH";
    public static final int SECRET_CODE_LENGTH = 6;
}
