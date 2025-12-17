package com.dev.lib.util.limiter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 并发限流器
 * 用于限制同时执行的操作数量，适合保护下游资源
 */
@Slf4j
public class ConcurrencyLimiter {

    private final String name;

    private final Semaphore semaphore;

    private final int maxConcurrency;

    private final Duration timeout;

    private ConcurrencyLimiter(String name, int maxConcurrency, Duration timeout) {

        this.name = name;
        this.maxConcurrency = maxConcurrency;
        this.semaphore = new Semaphore(maxConcurrency);
        this.timeout = timeout;
    }

    public static Builder builder() {

        return new Builder();
    }

    /**
     * 执行受限流保护的操作
     */
    public <T> T execute(Callable<T> callable) {

        if (!tryAcquire()) {
            throw new ConcurrencyLimitExceededException(name, maxConcurrency);
        }
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }

    /**
     * 执行受限流保护的操作（带降级）
     */
    public <T> T executeWithFallback(Callable<T> callable, Supplier<T> fallback) {

        try {
            return execute(callable);
        } catch (ConcurrencyLimitExceededException e) {
            log.debug("Concurrency limiter [{}] rejected, using fallback", name);
            return fallback.get();
        }
    }

    /**
     * 执行无返回值的操作
     */
    public void executeVoid(Runnable runnable) {

        execute(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 阻塞等待直到获取许可
     */
    public <T> T executeBlocking(Callable<T> callable) throws InterruptedException {

        semaphore.acquire();
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            semaphore.release();
        }
    }

    private boolean tryAcquire() {

        try {
            if (timeout != null && !timeout.isZero()) {
                return semaphore.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            return semaphore.tryAcquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // --- 限流异常 ---

    @Getter
    public static class ConcurrencyLimitExceededException extends RuntimeException {

        private final String name;

        private final int limit;

        public ConcurrencyLimitExceededException(String name, int limit) {

            super(String.format("Concurrency limiter [%s] exceeded limit of %d", name, limit));
            this.name = name;
            this.limit = limit;
        }
    }

    // --- Builder ---

    public static class Builder {

        private String name = "default";

        private int maxConcurrency = 10;

        private Duration timeout = Duration.ZERO;

        public Builder name(String name) {

            this.name = name;
            return this;
        }

        public Builder maxConcurrency(int maxConcurrency) {

            this.maxConcurrency = maxConcurrency;
            return this;
        }

        /**
         * 等待超时时间，默认不等待
         */
        public Builder timeout(Duration timeout) {

            this.timeout = timeout;
            return this;
        }

        public ConcurrencyLimiter build() {

            return new ConcurrencyLimiter(name, maxConcurrency, timeout);
        }
    }
}