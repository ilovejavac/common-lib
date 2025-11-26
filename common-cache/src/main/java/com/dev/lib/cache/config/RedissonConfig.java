package com.dev.lib.cache.config;

import com.dev.lib.cache.FastJson2JsonRedissonSerializer;
import lombok.RequiredArgsConstructor;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@EnableConfigurationProperties(RedisProperties.class)
@RequiredArgsConstructor
public class RedissonConfig {

    @Bean
    public RedissonAutoConfigurationCustomizer redissonCustomizer() {
        return config -> {
            config.setThreads(Runtime.getRuntime().availableProcessors() * 2);
            config.setNettyThreads(Runtime.getRuntime().availableProcessors() * 2);

            config.setCodec(new FastJson2JsonRedissonSerializer());
        };
    }
}