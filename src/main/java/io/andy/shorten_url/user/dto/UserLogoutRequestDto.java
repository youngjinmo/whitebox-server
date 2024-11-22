package io.andy.shorten_url.user.dto;

public record UserLogoutRequestDto(String accessToken) {
    public static UserLogoutRequestDto build(String accessToken) {
        return new UserLogoutRequestDto(accessToken);
    }
}
