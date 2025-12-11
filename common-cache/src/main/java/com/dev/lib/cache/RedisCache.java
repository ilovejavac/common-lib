package com.dev.lib.cache;

import com.dev.lib.cache.common.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;
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
@SuppressWarnings("all")
public class RedisCache implements InitializingBean {

    private final RedissonClient redissonClient;

    private static final String   CACHE_PREFIX   = "cache:";

    private static final String   NULL_MARKER    = "__NULL__";

    private static final Duration DEFAULT_TTL    = Duration.ofHours(1);

    private static final Duration NULL_CACHE_TTL = Duration.ofHours(5);

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
        return new CacheKey(keyStr);
    }

    public static void deletePattern(String pattern) {

        instance.redissonClient.getKeys().deleteByPattern(pattern);
    }

    // ============ CacheKey ============

    public static class CacheKey {

        private final String   key;

        private       Duration ttl = DEFAULT_TTL;

        CacheKey(String key) {

            this.key = key;
        }

        public CacheKey ttl(Duration ttl) {

            this.ttl = ttl;
            return this;
        }

        // --- 单值 ---

        public <T> CacheValue<T> get() {

            RBucket<Object> bucket = instance.redissonClient.getBucket(key);
            Object          raw    = bucket.get();
            if (raw == null) {
                return new CacheValue<>(
                        null,
                        false,
                        key,
                        ttl
                );
            }
            if (NULL_MARKER.equals(raw)) {
                return new CacheValue<>(
                        null,
                        true,
                        key,
                        ttl
                );
            }
            return new CacheValue<>(
                    (T) raw,
                    true,
                    key,
                    ttl
            );
        }

        public <T> void set(T value) {

            RBucket<Object> bucket = instance.redissonClient.getBucket(key);
            if (value == null) {
                bucket.set(
                        NULL_MARKER,
                        NULL_CACHE_TTL
                );
            } else {
                if (ttl == null) {
                    bucket.set(value);
                } else {
                    bucket.set(
                            value,
                            ttl
                    );
                }
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

        // --- 数据结构 ---

        public <T> CacheList<T> list() {

            return new CacheList<>(
                    key,
                    ttl,
                    instance.redissonClient.getList(key)
            );
        }

        public <T> CacheSet<T> set() {

            return new CacheSet<>(
                    key,
                    ttl,
                    instance.redissonClient.getSet(key)
            );
        }

        public <T> CacheQueue<T> queue() {

            return new CacheQueue<>(
                    key,
                    ttl,
                    instance.redissonClient.getQueue(key)
            );
        }

        public <T> CacheBlockingQueue<T> blockingQueue() {

            return new CacheBlockingQueue<>(
                    key,
                    ttl,
                    instance.redissonClient.getBlockingQueue(key)
            );
        }

        public <T> CacheScoredSortedSet<T> scoredSortedSet() {

            return new CacheScoredSortedSet<>(
                    key,
                    ttl,
                    instance.redissonClient.getScoredSortedSet(key)
            );
        }

        public CacheMap map() {

            return new CacheMap(
                    key,
                    ttl,
                    instance.redissonClient.getMap(key)
            );
        }

        public CacheAtomicLong atomicLong() {

            return new CacheAtomicLong(
                    key,
                    ttl,
                    instance.redissonClient.getAtomicLong(key)
            );
        }

        public <T> RBloomFilter<T> bloomFilter() {

            RBloomFilter<T> filter = instance.redissonClient.getBloomFilter(key);
            if (ttl != null && filter.isExists()) {
                filter.expire(ttl);
            }
            return filter;
        }

        // --- 锁 ---

        public RLock lock() {

            return instance.redissonClient.getLock(key);
        }

        public RLock fairLock() {

            return instance.redissonClient.getFairLock(key);
        }

        public RReadWriteLock readWriteLock() {

            return instance.redissonClient.getReadWriteLock(key);
        }

        public RSemaphore semaphore() {

            return instance.redissonClient.getSemaphore(key);
        }

        public RCountDownLatch countDownLatch() {

            return instance.redissonClient.getCountDownLatch(key);
        }

    }

    // ============ CacheValue ============

    @Getter
    public static class CacheValue<T> {

        private final T        value;

        private final boolean  cached;

        private final String   key;

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

            return RedisDistributedLock.lock(
                    "cache_load:",
                    key
            ).execute(() -> {
                          RBucket<Object> bucket    = instance.redissonClient.getBucket(key);
                          Object          rechecked = bucket.get();
                          if (rechecked != null) {
                              if (NULL_MARKER.equals(rechecked)) {
                                  return null;
                              }
                              return (T) rechecked;
                          }

                          T data = loader.get();
                          if (data == null) {
                              bucket.set(
                                      NULL_MARKER,
                                      NULL_CACHE_TTL
                              );
                          } else {
                              if (ttl == null) {
                                  bucket.set(data);
                              } else {
                                  bucket.set(
                                          data,
                                          ttl
                                  );
                              }
                          }
                          return data;
                      }
            );
        }

    }

}