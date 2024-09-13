package io.andy.shorten_url.util.random;

public final class Constants {
    public static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    public static String UPPER_CASE = LOWER_CASE.toUpperCase();
    public static final String DIGITS = "0123456789";
    public static final String ALL_CHARACTERS = LOWER_CASE.concat(UPPER_CASE).concat(DIGITS);
}
