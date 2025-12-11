package com.dev.lib.util.retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * 通用重试器
 */
@Slf4j
@SuppressWarnings("all")
@RequiredArgsConstructor
public class Retryer {

    private final int                                      maxAttempts;

    private final Duration                                 delay;

    private final BackoffStrategy                          backoffStrategy;

    private final double                                   backoffMultiplier;

    private final Duration                                 maxDelay;

    private final ImmutableSet<Class<? extends Throwable>> retryableExceptions;

    private final Predicate<Throwable>                     retryPredicate;

    private final RetryListener                            listener;

    public static Builder builder() {

        return new Builder();
    }

    /**
     * 执行带重试的操作
     *
     * @param callable 需要执行的操作
     * @param <T>      返回类型
     * @return 操作结果
     */
    public <T> T execute(@NonNull Callable<T> callable) {

        Throwable lastException = null;
        long      currentDelay  = delay.toMillis();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = callable.call();
                if (attempt > 1 && listener != null) {
                    listener.onSuccess(attempt);
                }
                return result;

            } catch (Throwable e) {
                lastException = e;

                if (!shouldRetry(
                        e,
                        attempt
                )) {
                    log.debug(
                            "Exception not retryable or max attempts reached: {}",
                            e.getClass().getSimpleName()
                    );
                    break;
                }

                if (listener != null) {
                    listener.onRetry(
                            attempt,
                            e
                    );
                }

                log.debug(
                        "Attempt {} failed, retrying in {}ms. Error: {}",
                        attempt,
                        currentDelay,
                        e.getMessage()
                );

                sleep(currentDelay);
                currentDelay = calculateNextDelay(currentDelay);
            }
        }

        if (listener != null) {
            listener.onFailure(
                    maxAttempts,
                    lastException
            );
        }

        throw new RetryExhaustedException(
                maxAttempts,
                lastException
        );
    }

    /**
     * 执行无返回值的操作
     */
    public void executeVoid(@NonNull Runnable runnable) {

        execute(() -> {
            runnable.run();
            return null;
        });
    }

    private boolean shouldRetry(Throwable e, int attempt) {

        if (attempt >= maxAttempts) return false;

        // 自定义判断优先
        if (retryPredicate != null) {
            return retryPredicate.test(e);
        }

        // 检查异常类型
        if (retryableExceptions.isEmpty()) return true;

        return retryableExceptions.anySatisfy(clazz -> clazz.isInstance(e));
    }

    private long calculateNextDelay(long currentDelay) {

        long nextDelay = switch (backoffStrategy) {
            case FIXED -> currentDelay;
            case LINEAR -> (long) (currentDelay + delay.toMillis());
            case EXPONENTIAL -> (long) (currentDelay * backoffMultiplier);
        };
        return Math.min(
                nextDelay,
                maxDelay.toMillis()
        );
    }

    private void sleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Retry interrupted",
                    e
            );
        }
    }

    // --- 退避策略枚举 ---
    public enum BackoffStrategy {
        FIXED,       // 固定延迟
        LINEAR,      // 线性增长
        EXPONENTIAL  // 指数增长
    }

    // --- 监听器接口 ---
    public interface RetryListener {

        default void onRetry(int attempt, Throwable e) {

        }

        default void onSuccess(int attempt) {

        }

        default void onFailure(int totalAttempts, Throwable lastException) {

        }

    }

    // --- 重试耗尽异常 ---
    public static class RetryExhaustedException extends RuntimeException {

        private final int attempts;

        public RetryExhaustedException(int attempts, Throwable cause) {

            super(
                    String.format(
                            "Retry exhausted after %d attempts",
                            attempts
                    ),
                    cause
            );
            this.attempts = attempts;
        }

        public int getAttempts() {

            return attempts;
        }

    }

    // --- Builder ---
    public static class Builder {

        private int                             maxAttempts         = 3;

        private Duration                        delay               = Duration.ofMillis(500);

        private BackoffStrategy                 backoffStrategy     = BackoffStrategy.EXPONENTIAL;

        private double                          backoffMultiplier   = 1.7;

        private Duration                        maxDelay            = Duration.ofSeconds(30);

        private Set<Class<? extends Throwable>> retryableExceptions = new HashSet<>();

        private Predicate<Throwable>            retryPredicate;

        private RetryListener                   listener;

        public Builder maxAttempts(int maxAttempts) {

            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder delay(Duration delay) {

            this.delay = delay;
            return this;
        }

        public Builder backoff(BackoffStrategy strategy) {

            this.backoffStrategy = strategy;
            return this;
        }

        public Builder backoffMultiplier(double multiplier) {

            this.backoffMultiplier = multiplier;
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {

            this.maxDelay = maxDelay;
            return this;
        }

        @SafeVarargs
        public final Builder retryOn(Class<? extends Throwable>... exceptions) {

            for (Class<? extends Throwable> ex : exceptions) {
                retryableExceptions.add(ex);
            }
            return this;
        }

        public Builder retryIf(Predicate<Throwable> predicate) {

            this.retryPredicate = predicate;
            return this;
        }

        public Builder onRetry(BiConsumer<Integer, Throwable> handler) {

            this.listener = new RetryListener() {
                @Override
                public void onRetry(int attempt, Throwable e) {

                    handler.accept(
                            attempt,
                            e
                    );
                }
            };
            return this;
        }

        public Builder listener(RetryListener listener) {

            this.listener = listener;
            return this;
        }

        public Retryer build() {

            return new Retryer(
                    maxAttempts,
                    delay,
                    backoffStrategy,
                    backoffMultiplier,
                    maxDelay,
                    Sets.immutable.ofAll(retryableExceptions),
                    retryPredicate,
                    listener
            );
        }

    }

}