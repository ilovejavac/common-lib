package com.dev.lib.redis;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@SuppressWarnings("java:S6548")
public class RedisCache implements InitializingBean {

    private final RedissonClient redissonClient;

    private static final String CACHE_PREFIX = "cache:";
    private static final String NULL_MARKER = "__NULL__";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(5);

    private static RedisCache instance;

    public RedisCache(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void afterPropertiesSet() {
        instance = this;
    }

    // ============ 入口 ============

    public static CacheKey key(Object... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("keys must not be empty");
        }
        String keyStr = Arrays.stream(keys).map(String::valueOf).collect(Collectors.joining(":"));
        return new CacheKey(CACHE_PREFIX + keyStr);
    }

    public static void deletePattern(String pattern) {
        String fullPattern = CACHE_PREFIX + pattern;
        instance.redissonClient.getKeys().getKeysStreamByPattern(fullPattern)
                .forEach(k -> instance.redissonClient.getBucket(k).delete());
    }

    // ============ CacheKey ============

    public static class CacheKey {
        private final String key;
        private Duration ttl = DEFAULT_TTL;

        CacheKey(String key) {
            this.key = key;
        }

        public CacheKey ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public <T> CacheValue<T> get() {
            RBucket<Object> bucket = instance.redissonClient.getBucket(key);
            Object raw = bucket.get();
            if (raw == null) {
                return new CacheValue<>(null, false, key, ttl);
            }
            if (NULL_MARKER.equals(raw)) {
                return new CacheValue<>(null, true, key, ttl);
            }
            @SuppressWarnings("unchecked") T value = (T) raw;
            return new CacheValue<>(value, true, key, ttl);
        }

        public <T> void set(T value) {
            RBucket<Object> bucket = instance.redissonClient.getBucket(key);
            if (value == null) {
                bucket.set(NULL_MARKER, NULL_CACHE_TTL);
            } else {
                bucket.set(value, ttl);
            }
        }

        public boolean delete() {
            return instance.redissonClient.getBucket(key).delete();
        }

        public boolean exists() {
            return instance.redissonClient.getBucket(key).isExists();
        }

        public long getTtl() {
            long millis = instance.redissonClient.getBucket(key).remainTimeToLive();
            return Duration.ofMillis(millis).toSeconds();
        }
    }

    // ============ CacheValue ============

    @Getter
    public static class CacheValue<T> {
        private final T value;
        private final boolean cached;
        private final String key;
        private final Duration ttl;

        CacheValue(T value, boolean cached, String key, Duration ttl) {
            this.value = value;
            this.cached = cached;
            this.key = key;
            this.ttl = ttl;
        }

        public T value() {
            return value;
        }

        public T orElse(Supplier<T> loader) {
            if (cached) {
                return value;
            }

            return RedisDistributedLock.withLock(
                    "cache_load:" + key, () -> {
                        RBucket<Object> bucket = instance.redissonClient.getBucket(key);
                        Object rechecked = bucket.get();
                        if (rechecked != null) {
                            if (NULL_MARKER.equals(rechecked)) {
                                return null;
                            }
                            @SuppressWarnings("unchecked") T val = (T) rechecked;
                            return val;
                        }

                        T data = loader.get();
                        if (data == null) {
                            bucket.set(NULL_MARKER, NULL_CACHE_TTL);
                        } else {
                            bucket.set(data, ttl);
                        }
                        return data;
                    }
            );
        }
    }
}