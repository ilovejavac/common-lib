package com.dev.lib.cache;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@RequiredArgsConstructor
@SuppressWarnings("all")
public class RedisDistributedLock implements InitializingBean {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_WAIT_TIME = 3L;

    private static RedisDistributedLock instance;

    @Override
    public void afterPropertiesSet() throws Exception {
        instance = this;
    }

    public static LockBuilder lock(String... keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("keys must not be empty");
        }
        String keyStr = Arrays.stream(keys).map(String::valueOf).collect(Collectors.joining(":"));
        return new LockBuilder(LOCK_PREFIX + keyStr);
    }

    public static class LockBuilder {

        private final String key;
        private long waitTime = DEFAULT_WAIT_TIME;
        private long leaseTime = -1;
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        LockBuilder(String key) {
            this.key = key;
        }

        public LockBuilder waitTime(long waitTime) {
            this.waitTime = waitTime;
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

        public <T> T tryExecute(Supplier<T> block) {
            RLock lock = instance.redissonClient.getLock(key);
            try {
                boolean acquired = (leaseTime > 0)
                                   ? lock.tryLock(waitTime, leaseTime, timeUnit)
                                   : lock.tryLock(waitTime, timeUnit);
                if (!acquired) {
                    return null;
                }
                try {
                    log.debug("Acquired lock: {}", key);
                    return block.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Released lock: {}", key);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        public Boolean tryExecute(Runnable task) {
            return tryExecute(() -> {
                task.run();
                return true;
            }) != null;
        }

        public <T> T execute(Supplier<T> block) {
            RLock lock = instance.redissonClient.getLock(key);
            try {
                boolean acquired = (leaseTime > 0)
                                   ? lock.tryLock(waitTime, leaseTime, timeUnit)
                                   : lock.tryLock(waitTime, timeUnit);

                if (!acquired) {
                    throw new LockAcquisitionException("Failed to acquire lock: " + key);
                }

                try {
                    log.debug("Acquired lock: {}", key);
                    return block.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.debug("Released lock: {}", key);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("Lock acquisition interrupted: " + key, e);
            }
        }

        public void execute(Runnable block) {
            execute(() -> {
                block.run();
                return null;
            });
        }
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
