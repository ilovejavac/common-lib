package com.dev.lib.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
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

    // ============ 静态方法 - 单对象 ============

    public static <T> void store(Object value, Object... keys) {
        Object[] keyParts = Arrays.copyOf(keys, keys.length - 1);
        instance.storeValue(null, keyParts, value);
    }

    public static <T> void store(Duration ttl,Object value, Object... keysAndValue) {
        Object[] keyParts = Arrays.copyOf(keysAndValue, keysAndValue.length - 1);
        instance.storeValue(ttl, keyParts, value);
    }

    public static <T> T load(Class<T> clazz, Object... keys) {
        return instance.loadValue(clazz, keys);
    }

    public static <T> List<T> loadList(Class<T> elementClass, Object... keys) {
        return instance.loadListValue(elementClass, keys);
    }

    public static <T> T load(TypeReference<T> typeRef, Object... keys) {
        return instance.loadValue(typeRef, keys);
    }

    public static <T> T getIfPresent(Class<T> clazz, Supplier<T> loader, Object... keys) {
        return instance.getOrLoad(clazz, null, loader, keys);
    }

    public static <T> T getIfPresent(Class<T> clazz, Duration ttl, Supplier<T> loader, Object... keys) {
        return instance.getOrLoad(clazz, ttl, loader, keys);
    }

    public static <T> List<T> getIfPresentList(Class<T> elementClass, Supplier<List<T>> loader, Object... keys) {
        return instance.getOrLoadList(elementClass, null, loader, keys);
    }

    public static <T> List<T> getIfPresentList(
            Class<T> elementClass,
            Duration ttl,
            Supplier<List<T>> loader,
            Object... keys
    ) {
        return instance.getOrLoadList(elementClass, ttl, loader, keys);
    }

    public static boolean remove(Object... keys) {
        String storeKey = instance.buildKey(keys);
        return instance.redissonClient.getBucket(storeKey).delete();
    }

    public static boolean exists(Object... keys) {
        String storeKey = instance.buildKey(keys);
        return instance.redissonClient.getBucket(storeKey).isExists();
    }

    // ============ 实例方法 ============

    public <T> void storeValue(Duration ttl, Object[] keys, T value) {
        String storeKey = buildKey(keys);
        RBucket<String> bucket = redissonClient.getBucket(storeKey);
        try {
            String json = objectMapper.writeValueAsString(value);
            if (ttl != null) {
                bucket.set(json, ttl.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                bucket.set(json);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize value", e);
        }
    }

    public <T> T loadValue(Class<T> clazz, Object... keys) {
        String storeKey = buildKey(keys);
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

    public <T> List<T> loadListValue(Class<T> elementClass, Object... keys) {
        String storeKey = buildKey(keys);
        RBucket<String> bucket = redissonClient.getBucket(storeKey);
        String json = bucket.get();
        if (json == null) {
            return null;
        }
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, elementClass);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize list", e);
        }
    }

    public <T> T loadValue(TypeReference<T> typeRef, Object... keys) {
        String storeKey = buildKey(keys);
        RBucket<String> bucket = redissonClient.getBucket(storeKey);
        String json = bucket.get();
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize value", e);
        }
    }

    public <T> T getOrLoad(Class<T> clazz, Duration ttl, Supplier<T> loader, Object... keys) {
        String storeKey = buildKey(keys);
        RBucket<String> bucket = redissonClient.getBucket(storeKey);

        String cached = bucket.get();
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, clazz);
            } catch (Exception e) {
                log.error("Failed to deserialize value, reloading", e);
            }
        }

        return RedisDistributedLock.withLock(
                "store_load:" + storeKey,
                () -> {
                    String rechecked = bucket.get();
                    if (rechecked != null) {
                        try {
                            return objectMapper.readValue(rechecked, clazz);
                        } catch (Exception e) {
                            log.error("Failed to deserialize value on recheck", e);
                        }
                    }

                    T data = loader.get();
                    if (data != null) {
                        try {
                            String json = objectMapper.writeValueAsString(data);
                            if (ttl != null) {
                                bucket.set(json, ttl.toMillis(), TimeUnit.MILLISECONDS);
                            } else {
                                bucket.set(json);
                            }
                        } catch (Exception e) {
                            log.error("Failed to serialize value", e);
                        }
                    }
                    return data;
                }
        );
    }

    public <T> List<T> getOrLoadList(Class<T> elementClass, Duration ttl, Supplier<List<T>> loader, Object... keys) {
        String storeKey = buildKey(keys);
        RBucket<String> bucket = redissonClient.getBucket(storeKey);

        String cached = bucket.get();
        if (cached != null) {
            try {
                JavaType type = objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, elementClass);
                return objectMapper.readValue(cached, type);
            } catch (Exception e) {
                log.error("Failed to deserialize list, reloading", e);
            }
        }

        return RedisDistributedLock.withLock(
                "store_load:" + storeKey,
                () -> {
                    String rechecked = bucket.get();
                    if (rechecked != null) {
                        try {
                            JavaType type = objectMapper.getTypeFactory()
                                    .constructCollectionType(List.class, elementClass);
                            return objectMapper.readValue(rechecked, type);
                        } catch (Exception e) {
                            log.error("Failed to deserialize list on recheck", e);
                        }
                    }

                    List<T> data = loader.get();
                    if (data != null) {
                        try {
                            String json = objectMapper.writeValueAsString(data);
                            if (ttl != null) {
                                bucket.set(json, ttl.toMillis(), TimeUnit.MILLISECONDS);
                            } else {
                                bucket.set(json);
                            }
                        } catch (Exception e) {
                            log.error("Failed to serialize list", e);
                        }
                    }
                    return data;
                }
        );
    }

    private String buildKey(Object... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("keys must not be empty");
        }
        String keyStr = Arrays.stream(keys)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
        return STORE_PREFIX + keyStr;
    }
}