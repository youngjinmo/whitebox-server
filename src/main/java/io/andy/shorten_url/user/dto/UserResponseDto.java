package io.andy.shorten_url.user.dto;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.entity.User;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String username,
        UserState state,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt,
        LocalDateTime withdrawnAt,
        LocalDateTime deletedAt
) {
   public UserResponseDto(User user) {
       this(
               user.getId(),
               user.getUsername(),
               user.getState(),
               user.getRole(),
               user.getCreatedAt(),
               user.getUpdatedAt(),
               user.getLastLoginAt(),
               user.getWithdrawnAt(),
               user.getDeletedAt()
       );
   }
}
