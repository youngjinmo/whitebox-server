package io.andy.shorten_url.user.dto;

public record CreateUserServiceDto(String username, String password) {
    public static CreateUserServiceDto build(String username, String password) {
        return new CreateUserServiceDto(username, password);
    }
    public static CreateUserServiceDto from(CreateUserRequestDto requestDto) {
        return new CreateUserServiceDto(requestDto.username(), requestDto.password());
    }
}
