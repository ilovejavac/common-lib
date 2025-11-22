package com.dev.lib.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@EnableConfigurationProperties(RedisProperties.class)
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(RedisProperties properties) {
        Config config = new Config();

        if (properties.getCluster() != null && properties.getCluster().getNodes() != null) {
            // 集群模式
            List<String> nodes = properties.getCluster().getNodes().stream()
                    .map(node -> "redis://" + node)
                    .toList();

            config.useClusterServers()
                    .addNodeAddress(nodes.toArray(new String[0]))
                    .setPassword(properties.getPassword())
                    .setScanInterval(2000)
                    .setMasterConnectionMinimumIdleSize(1)
                    .setMasterConnectionPoolSize(10)
                    .setConnectTimeout(10000)
                    .setTimeout(3000)
                    .setRetryAttempts(3)
                    .setPingConnectionInterval(30000);
        } else {
            // 单机模式
            String address = String.format("redis://%s:%d", properties.getHost(), properties.getPort());

            config.useSingleServer()
                    .setAddress(address)
                    .setPassword(properties.getPassword())
                    .setDatabase(properties.getDatabase())
                    .setConnectionMinimumIdleSize(1)
                    .setConnectionPoolSize(10)
                    .setConnectTimeout(10000)
                    .setTimeout(3000)
                    .setRetryAttempts(3)
                    .setPingConnectionInterval(30000);
        }

        config.setCodec(new JsonJacksonCodec());
        return Redisson.create(config);
    }
}