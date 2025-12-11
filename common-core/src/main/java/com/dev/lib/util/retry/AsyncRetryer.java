package com.dev.lib.util.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 异步重试器 (基于 CompletableFuture)
 */
@Slf4j
@RequiredArgsConstructor
public class AsyncRetryer {

    private final Retryer retryer;

    private final ScheduledExecutorService scheduler;

    public static AsyncRetryer of(Retryer retryer, ScheduledExecutorService scheduler) {

        return new AsyncRetryer(
                retryer,
                scheduler
        );
    }

    /**
     * 异步执行带重试的操作
     */
    public <T> CompletableFuture<T> executeAsync(Supplier<T> supplier) {

        return CompletableFuture.supplyAsync(
                () -> retryer.execute(supplier::get),
                scheduler
        );
    }

    /**
     * 延迟执行
     */
    public <T> CompletableFuture<T> executeWithDelay(Supplier<T> supplier, Duration initialDelay) {

        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.schedule(
                () -> {
                    try {
                        T result = retryer.execute(supplier::get);
                        future.complete(result);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                },
                initialDelay.toMillis(),
                TimeUnit.MILLISECONDS
        );
        return future;
    }

}