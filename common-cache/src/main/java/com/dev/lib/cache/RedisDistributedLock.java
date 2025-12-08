package com.dev.lib.cache;

import com.dev.lib.util.retry.Retryer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

    // ============ 入口 ============

    public static LockBuilder lock(String... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("keys must not be empty");
        }
        String keyStr = Arrays.stream(keys).map(String::valueOf).collect(Collectors.joining(":"));
        return new LockBuilder(LOCK_PREFIX + keyStr);
    }

    // ============ LockBuilder ============

    public static class LockBuilder {
        private final String key;
        private long waitTime = DEFAULT_WAIT_TIME;
        private int retryTimes = DEFAULT_RETRY_TIMES;
        private long leaseTime = -1;
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        LockBuilder(String key) {
            this.key = key;
        }

        public LockBuilder waitTime(long waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public LockBuilder retryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
            return this;
        }

        public LockBuilder leaseTime(long leaseTime) {
            this.leaseTime = leaseTime;
            return this;
        }

        public LockBuilder timeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public <T> T execute(Supplier<T> block) {
            String lockKey = LOCK_PREFIX + key;
            RLock lock = instance.redissonClient.getLock(lockKey);

            // 构建重试器
            Retryer retryer = Retryer.builder()
                    .maxDelay(Duration.ofSeconds(1))
                    .retryOn(LockNotAcquiredException.class)
                    .onRetry((attempt, e) -> log.debug("Lock retry attempt {}: {}", attempt, lockKey))
                    .build();

            try {
                return retryer.execute(() -> {
                    boolean acquired = (leaseTime > 0)
                            ? lock.tryLock(waitTime, leaseTime, timeUnit)
                            : lock.tryLock(waitTime, timeUnit);

                    if (!acquired) {
                        throw new LockNotAcquiredException("Lock not acquired: " + lockKey);
                    }

                    try {
                        log.debug("Acquired lock: {}", lockKey);
                        return block.get();
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                            log.debug("Released lock: {}", lockKey);
                        }
                    }
                });

            } catch (Retryer.RetryExhaustedException e) {
                throw new LockAcquisitionException("Failed to acquire lock after " + retryTimes + " retries: " + key);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Lock acquisition interrupted: " + key, e.getCause());
                }
                throw e;
            }
        }

        public void execute(Runnable block) {
            execute(() -> {
                block.run();
                return null;
            });
        }
    }

    // 内部异常：用于触发重试
    private static class LockNotAcquiredException extends RuntimeException {
        LockNotAcquiredException(String message) {
            super(message);
        }
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}