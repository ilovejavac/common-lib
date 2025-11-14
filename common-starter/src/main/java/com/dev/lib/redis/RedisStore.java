package com.dev.lib.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(RedissonClient.class)
@RequiredArgsConstructor
public class RedisStore {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String STORE_PREFIX = "store:";
    private static RedisStore instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static <T> void store(String key, T value) {
        instance.storeValue(key, value);
    }

    public static <T> T load(String key, Class<T> clazz) {
        return instance.loadValue(key, clazz);
    }

    public static boolean remove(String key) {
        String storeKey = STORE_PREFIX + key;
        return instance.redissonClient.getBucket(storeKey).delete();
    }

    public static boolean exists(String key) {
        String storeKey = STORE_PREFIX + key;
        return instance.redissonClient.getBucket(storeKey).isExists();
    }

    public <T> void storeValue(String key, T value) {
        String storeKey = STORE_PREFIX + key;
        RBucket<String> bucket = redissonClient.getBucket(storeKey);
        try {
            String json = objectMapper.writeValueAsString(value);
            bucket.set(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize value", e);
        }
    }

    public <T> T loadValue(String key, Class<T> clazz) {
        String storeKey = STORE_PREFIX + key;
        RBucket<String> bucket = redissonClient.getBucket(storeKey);
        String json = bucket.get();
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize value", e);
        }
    }
}