package io.andy.shorten_url.user.repository;

import io.andy.shorten_url.common.CommonRepository;
import io.andy.shorten_url.user.entity.User;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, CommonRepository<User> {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    @Override @Profile("test") void deleteAll();
}
