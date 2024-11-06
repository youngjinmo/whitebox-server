package io.andy.shorten_url.user.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.entity.User;

public record UserLoginResponseDto(
        Long id,
        String username,
        UserState state,
        UserRole role,
        String accessToken
) {
   public UserLoginResponseDto(User user, String accessToken) {
       this(
               user.getId(),
               user.getUsername(),
               user.getState(),
               user.getRole(),
               accessToken
       );
   }
}
