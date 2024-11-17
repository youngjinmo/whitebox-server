package io.andy.shorten_url.user.entity;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Getter @Setter
@ToString(exclude = "password")
public class User implements UserDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    private String password;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserState state;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime updatedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime deletedAt;

    protected User() {}

    public User(String username,
                String password,
                UserState state,
                UserRole role
    ) {
        this.username = username;
        this.password = password;
        this.state = state;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role.name()));
        return authorities;
    }
}
