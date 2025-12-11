package com.dev.lib.cache;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class RedisStore {

    private static final String   STORE_PREFIX = "store:";

    private static final Duration PERSISTENT   = null;

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