package io.andy.shorten_url.config;

import io.andy.shorten_url.auth.AuthSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.database}")
    private int database;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(database);
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
        connectionFactory.getConnection().ping();
        return connectionFactory;
    }

    @Bean
    public RedisTemplate<String, AuthSession> redisTemplate() {
        RedisTemplate<String, AuthSession> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // JSON 직렬화 설정
        Jackson2JsonRedisSerializer<AuthSession> serializer = new Jackson2JsonRedisSerializer<>(AuthSession.class);
        redisTemplate.setValueSerializer(serializer);

        redisTemplate.setConnectionFactory(redisConnectionFactory());
        return redisTemplate;
    }
}
