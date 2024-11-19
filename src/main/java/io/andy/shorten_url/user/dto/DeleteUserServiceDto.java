package io.andy.shorten_url.user.dto;

public record DeleteUserServiceDto(Long id, String ipAddress, String userAgent) {
    public static DeleteUserServiceDto of(Long id, String ipAddress, String userAgent) {
        return new DeleteUserServiceDto(id, ipAddress, userAgent);
    }
}
