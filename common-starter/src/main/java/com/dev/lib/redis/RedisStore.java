package com.dev.lib.redis;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RQueue;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSemaphore;
import org.redisson.api.RSet;
import org.redisson.api.RSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@SuppressWarnings("java:S6548")
public class RedisStore implements InitializingBean {

    private final RedissonClient redissonClient;

    private static final String STORE_PREFIX = "store:";
    private static RedisStore instance;

    public RedisStore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void afterPropertiesSet() {
        instance = this;
    }

    // ============ 入口 ============

    public static StoreKey key(Object... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("keys must not be empty");
        }
        String keyStr = Arrays.stream(keys)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
        return new StoreKey(STORE_PREFIX + keyStr);
    }

    // ============ StoreKey ============

    public static class StoreKey {
        private final String key;

        StoreKey(String key) {
            this.key = key;
        }

        // --- 单值 ---

        public <T> StoreValue<T> get() {
            T value = instance.redissonClient.<T>getBucket(key).get();
            return new StoreValue<>(value, key);
        }

        public <T> void set(T value) {
            instance.redissonClient.<T>getBucket(key).set(value);
        }

        public boolean delete() {
            return instance.redissonClient.getBucket(key).delete();
        }

        public boolean exists() {
            return instance.redissonClient.getBucket(key).isExists();
        }

        // --- 数据结构 ---

        public StoreMap map() {
            return new StoreMap(key);
        }

        public <T> RList<T> list() {
            return instance.redissonClient.getList(key);
        }

        public <T> RSet<T> set() {
            return instance.redissonClient.getSet(key);
        }

        public <T> RQueue<T> queue() {
            return instance.redissonClient.getQueue(key);
        }

        public <T> RBlockingQueue<T> blockingQueue() {
            return instance.redissonClient.getBlockingQueue(key);
        }

        public <T> RSortedSet<T> sortedSet() {
            return instance.redissonClient.getSortedSet(key);
        }

        public <T> RScoredSortedSet<T> scoredSortedSet() {
            return instance.redissonClient.getScoredSortedSet(key);
        }

        public RAtomicLong atomicLong() {
            return instance.redissonClient.getAtomicLong(key);
        }

        public <T> RBloomFilter<T> bloomFilter() {
            return instance.redissonClient.getBloomFilter(key);
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

    // ============ StoreValue ============

    public static class StoreValue<T> {
        private final T value;
        private final String key;

        StoreValue(T value, String key) {
            this.value = value;
            this.key = key;
        }

        public T value() {
            return value;
        }

        public T orElse(Supplier<T> loader) {
            if (value != null) {
                return value;
            }
            T data = loader.get();
            if (data != null) {
                instance.redissonClient.<T>getBucket(key).set(data);
            }
            return data;
        }

        public T orElseWithLock(Supplier<T> loader) {
            if (value != null) {
                return value;
            }
            return RedisDistributedLock.withLock(
                    "store_load:" + key,
                    () -> {
                        T rechecked = instance.redissonClient.<T>getBucket(key).get();
                        if (rechecked != null) {
                            return rechecked;
                        }
                        T data = loader.get();
                        if (data != null) {
                            instance.redissonClient.<T>getBucket(key).set(data);
                        }
                        return data;
                    }
            );
        }
    }

    // ============ StoreMap ============

    public static class StoreMap {
        private final String key;

        StoreMap(String key) {
            this.key = key;
        }

        public <K, V> RMap<K, V> raw() {
            return instance.redissonClient.getMap(key);
        }

        public <V> V get(Object field) {
            return instance.redissonClient.<Object, V>getMap(key).get(field);
//            return new StoreMapValue<>(value, key, field);
        }

        public <V> void set(Object field, V value) {
            instance.redissonClient.<Object, V>getMap(key).put(field, value);
        }

        public boolean delete(Object field) {
            return instance.redissonClient.getMap(key).remove(field) != null;
        }

        public boolean exists(Object field) {
            return instance.redissonClient.getMap(key).containsKey(field);
        }

        public boolean deleteAll() {
            return instance.redissonClient.getMap(key).delete();
        }

        public int size() {
            return instance.redissonClient.getMap(key).size();
        }
    }

//    // ============ StoreMapValue ============
//
//    public static class StoreMapValue<V> {
//        private final V value;
//        private final String key;
//        private final Object field;
//
//        StoreMapValue(V value, String key, Object field) {
//            this.value = value;
//            this.key = key;
//            this.field = field;
//        }
//
//        public V value() {
//            return value;
//        }
//
//        public V orElse(Supplier<V> loader) {
//            if (value != null) {
//                return value;
//            }
//            V data = loader.get();
//            if (data != null) {
//                instance.redissonClient.<Object, V>getMap(key).put(field, data);
//            }
//            return data;
//        }
//    }
}