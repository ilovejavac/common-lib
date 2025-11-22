package com.dev.lib.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@RequiredArgsConstructor
@SuppressWarnings("all")
public class RedisDistributedLock {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_WAIT_TIME = 3L;
    private static final int DEFAULT_RETRY_TIMES = 3;
    private static final long RETRY_INTERVAL_MS = 100L;

    private static RedisDistributedLock instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static <T> T withLock(
            String key,
            long waitTime,
            int retryTimes,
            long leaseTime,
            TimeUnit timeUnit,
            Supplier<T> block
    ) {
        return instance.executeWithLock(key, waitTime, retryTimes, leaseTime, timeUnit, block);
    }

    public static <T> T withLock(String key, Supplier<T> block) {
        return instance.executeWithLock(key, DEFAULT_WAIT_TIME, DEFAULT_RETRY_TIMES, -1, TimeUnit.SECONDS, block);
    }

    public static <T> T withLock(String key, long waitTime, int retryTimes, Supplier<T> block) {
        return instance.executeWithLock(key, waitTime, retryTimes, -1, TimeUnit.SECONDS, block);
    }

    public <T> T executeWithLock(
            String key, long waitTime, int retryTimes, long leaseTime,
            TimeUnit timeUnit, Supplier<T> block
    ) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Lock key must not be blank");
        }

        String lockKey = LOCK_PREFIX + key;
        RLock lock = redissonClient.getLock(lockKey);
        int attempts = 0;

        while (attempts < retryTimes) {
            try {
                boolean acquired = (leaseTime > 0)
                        ? lock.tryLock(waitTime, leaseTime, timeUnit)
                        : lock.tryLock(waitTime, timeUnit);

                if (acquired) {
                    try {
                        log.debug("Acquired lock: {}", lockKey);
                        return block.get();
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                            log.debug("Released lock: {}", lockKey);
                        }
                    }
                }

                attempts++;
                if (attempts < retryTimes) {
                    long sleepTime = Math.min(RETRY_INTERVAL_MS * (1L << (attempts - 1)), 1000L);
                    Thread.sleep(sleepTime);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Lock acquisition interrupted: " + key, e);
            }
        }

        throw new LockAcquisitionException("Failed to acquire lock after " + retryTimes + " retries: " + key);
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}