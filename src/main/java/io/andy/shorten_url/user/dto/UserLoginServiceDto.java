package io.andy.shorten_url.user.dto;

public record UserLoginServiceDto(String username, String password, String ipAddress, String userAgent) {
    public static UserLoginServiceDto build(String username, String password, String ipAddress, String userAgent) {
        return new UserLoginServiceDto(username, password, ipAddress, userAgent);
    }
    public static UserLoginServiceDto build(UserLoginRequestDto requestDto, String ipAddress, String userAgent) {
        return new UserLoginServiceDto(requestDto.username(), requestDto.password(), ipAddress, userAgent);
    }
}
