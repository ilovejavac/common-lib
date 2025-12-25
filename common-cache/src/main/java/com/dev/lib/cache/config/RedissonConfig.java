package com.dev.lib.cache.config;

import com.dev.lib.cache.FastJson2JsonRedissonSerializer;
import lombok.RequiredArgsConstructor;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@RequiredArgsConstructor
public class RedissonConfig {

    @Bean
    public RedissonAutoConfigurationCustomizer redissonAutoConfigurationCustomizer() {

        return config -> {
            config.setCodec(new FastJson2JsonRedissonSerializer());
        };
    }

}