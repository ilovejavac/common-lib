package com.dev.lib.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
public class RedisStore {

    private static final String STORE_PREFIX = "store:";
    private static final Duration PERSISTENT = null;

    public static RedisCache.CacheKey key(Object... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("keys must not be empty");
        }
        String keyStr = STORE_PREFIX + Arrays.stream(keys)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
        return RedisCache.key(keyStr).ttl(PERSISTENT);
    }
}