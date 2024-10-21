package io.andy.shorten_url.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Builder
@Getter @Setter
@AllArgsConstructor
@RedisHash
public class AuthSession implements Serializable {
    private final String prefix;
    private final String value;
}
