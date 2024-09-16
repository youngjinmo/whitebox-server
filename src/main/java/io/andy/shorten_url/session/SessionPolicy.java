package io.andy.shorten_url.session;

public final class SessionPolicy {
    private SessionPolicy() {}
    public static final int SESSION_ACTIVE_TIME = 60 * 60 * 24;
    public static final String SESSION_KEY_LOGIN = "AUTH";
}
