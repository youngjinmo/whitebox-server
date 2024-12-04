package io.andy.shorten_url.user.dto;

public record FindPasswordDto(String username, String serverDomain, String port) {
    public static FindPasswordDto build(String username, String serverDomain, String port) {
        return new FindPasswordDto(username, serverDomain, port);
    }
}
