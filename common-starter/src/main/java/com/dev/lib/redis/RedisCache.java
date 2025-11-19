package com.dev.lib.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@RequiredArgsConstructor
public class RedisCache {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String CACHE_PREFIX = "cache:";
    private static final String NULL_MARKER = "__NULL__";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(5);

    private static RedisCache instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static <T> T get(String key, Class<T> clazz, Supplier<T> loader) {
        return instance.getCache(key, DEFAULT_TTL, clazz, loader);
    }

    public static <T> T get(String key, Duration ttl, Class<T> clazz, Supplier<T> loader) {
        return instance.getCache(key, ttl, clazz, loader);
    }

    public static <T> void set(String key, T value, Duration ttl) {
        instance.setCache(key, value, ttl);
    }

    public static boolean delete(String key) {
        return instance.deleteCache(key);
    }

    public static boolean exists(String key) {
        return instance.cacheExists(key);
    }

    // ============ 实例方法 ============

    public <T> T getCache(String key, Duration ttl, Class<T> clazz, Supplier<T> loader) {
        String cacheKey = CACHE_PREFIX + key;
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);

        String cached = bucket.get();
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                return null;
            }
            try {
                return objectMapper.readValue(cached, clazz);
            } catch (Exception e) {
                log.error("Failed to deserialize cache", e);
            }
        }

        return RedisDistributedLock.withLock(
                "cache_load:" + key, () -> {
                    String rechecked = bucket.get();
                    if (rechecked != null) {
                        if (NULL_MARKER.equals(rechecked)) {
                            return null;
                        }
                        try {
                            return objectMapper.readValue(rechecked, clazz);
                        } catch (Exception e) {
                            log.error("Failed to deserialize cache", e);
                        }
                    }

                    T data = loader.get();

                    try {
                        String valueToCache = (data == null) ? NULL_MARKER : objectMapper.writeValueAsString(data);
                        Duration actualTtl = (data == null) ? NULL_CACHE_TTL : ttl;
                        bucket.set(valueToCache, actualTtl.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("Failed to serialize cache", e);
                    }

                    return data;
                }
        );
    }

    public <T> void setCache(String key, T value, Duration ttl) {
        String cacheKey = CACHE_PREFIX + key;
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);

        try {
            String valueToCache = (value == null) ? NULL_MARKER : objectMapper.writeValueAsString(value);
            Duration actualTtl = (value == null) ? NULL_CACHE_TTL : ttl;
            bucket.set(valueToCache, actualTtl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set cache", e);
        }
    }

    public boolean deleteCache(String key) {
        String cacheKey = CACHE_PREFIX + key;
        return redissonClient.getBucket(cacheKey).delete();
    }

    public boolean cacheExists(String key) {
        String cacheKey = CACHE_PREFIX + key;
        return redissonClient.getBucket(cacheKey).isExists();
    }

    public void deleteCachePattern(String pattern) {
        String fullPattern = CACHE_PREFIX + pattern;
        redissonClient.getKeys().getKeysStreamByPattern(fullPattern)
                .forEach(key -> redissonClient.getBucket(key).delete());
    }

    public long getTtl(String key) {
        String cacheKey = CACHE_PREFIX + key;
        RBucket<String> bucket = redissonClient.getBucket(cacheKey);
        return TimeUnit.MILLISECONDS.toSeconds(bucket.remainTimeToLive());
    }
}