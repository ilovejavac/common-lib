package com.dev.lib.cache.config;

import com.dev.lib.cache.FastJson2JsonRedissonSerializer;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@RequiredArgsConstructor
public class RedissonConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis")
    public RedissonProperties redissonProperties() {

        return new RedissonProperties();
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedissonProperties properties) {

        Config config = new Config();
        config.setCodec(new FastJson2JsonRedissonSerializer());

        String address = "redis://" + properties.getHost() + ":" + properties.getPort();

        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(properties.getDatabase());

        if (properties.getPassword() != null && !properties.getPassword().isEmpty()) {
            serverConfig.setPassword(properties.getPassword());
        }

        return Redisson.create(config);
    }

}