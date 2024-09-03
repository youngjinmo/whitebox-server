package io.andy.shorten_url;

import io.andy.shorten_url.user.repository.UserRepository;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigService {

    private UserRepository userRepository;

    public ConfigService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
