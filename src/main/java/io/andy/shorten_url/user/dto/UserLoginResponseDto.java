package io.andy.shorten_url.user.dto;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.entity.User;

public record UserLoginResponseDto(
        Long id,
        String username,
        UserState state,
        UserRole role,
        String accessToken,
        String refreshToken
) {
   public static UserLoginResponseDto build(User user, TokenResponseDto tokenResponseDto) {
       return new UserLoginResponseDto(
               user.getId(),
               user.getUsername(),
               user.getState(),
               user.getRole(),
               tokenResponseDto.accessToken(),
               tokenResponseDto.refreshToken()
       );
   }
}
