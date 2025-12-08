package com.dev.lib.util.breaker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 断路器实现
 * 支持三种状态：CLOSED（正常）、OPEN（熔断）、HALF_OPEN（半开）
 */
@Slf4j
@SuppressWarnings("all")
public class CircuitBreaker {

    private final String name;
    private final int failureThreshold;
    private final Duration timeout;
    private final int halfOpenRequests;
    private final StateChangeListener listener;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong openTime = new AtomicLong(0);

    private CircuitBreaker(String name, int failureThreshold, Duration timeout,
                           int halfOpenRequests, StateChangeListener listener) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeout = timeout;
        this.halfOpenRequests = halfOpenRequests;
        this.listener = listener;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 执行受保护的操作
     */
    public <T> T execute(Callable<T> callable) {
        if (!allowRequest()) {
            throw new CircuitBreakerOpenException(name, getRemainingTimeout());
        }

        try {
            T result = callable.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行受保护的操作（带降级）
     */
    public <T> T executeWithFallback(Callable<T> callable, Supplier<T> fallback) {
        try {
            return execute(callable);
        } catch (CircuitBreakerOpenException e) {
            log.debug("Circuit breaker [{}] is open, using fallback", name);
            return fallback.get();
        } catch (Exception e) {
            log.debug("Execution failed, using fallback. Error: {}", e.getMessage());
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
     * 是否允许请求通过
     */
    public boolean allowRequest() {
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                if (isTimeoutElapsed()) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        halfOpenAttempts.set(0);
                        notifyStateChange(State.OPEN, State.HALF_OPEN);
                        log.info("Circuit breaker [{}] transitioned to HALF_OPEN", name);
                    }
                    return true;
                }
                return false;

            case HALF_OPEN:
                return halfOpenAttempts.incrementAndGet() <= halfOpenRequests;

            default:
                return false;
        }
    }

    private void onSuccess() {
        successCount.incrementAndGet();

        if (state.get() == State.HALF_OPEN) {
            if (successCount.get() >= halfOpenRequests) {
                reset();
            }
        } else {
            failureCount.set(0);
        }
    }

    private void onFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();

        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            tripBreaker();
        } else if (currentState == State.CLOSED && failures >= failureThreshold) {
            tripBreaker();
        }
    }

    private void tripBreaker() {
        State previousState = state.getAndSet(State.OPEN);
        if (previousState != State.OPEN) {
            openTime.set(System.currentTimeMillis());
            notifyStateChange(previousState, State.OPEN);
            log.warn("Circuit breaker [{}] tripped to OPEN after {} failures", name, failureCount.get());
        }
    }

    private void reset() {
        State previousState = state.getAndSet(State.CLOSED);
        if (previousState != State.CLOSED) {
            failureCount.set(0);
            successCount.set(0);
            halfOpenAttempts.set(0);
            notifyStateChange(previousState, State.CLOSED);
            log.info("Circuit breaker [{}] reset to CLOSED", name);
        }
    }

    private boolean isTimeoutElapsed() {
        return System.currentTimeMillis() - openTime.get() >= timeout.toMillis();
    }

    private Duration getRemainingTimeout() {
        long elapsed = System.currentTimeMillis() - openTime.get();
        long remaining = timeout.toMillis() - elapsed;
        return Duration.ofMillis(Math.max(0, remaining));
    }

    private void notifyStateChange(State from, State to) {
        if (listener != null) {
            listener.onStateChange(name, from, to);
        }
    }

    // --- Getters ---
    public State getState() {
        return state.get();
    }

    public String getName() {
        return name;
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * 手动重置断路器
     */
    public void forceReset() {
        reset();
    }

    /**
     * 手动打开断路器
     */
    public void forceOpen() {
        tripBreaker();
    }

    // --- 状态枚举 ---
    public enum State {
        CLOSED,     // 正常状态，请求正常通过
        OPEN,       // 熔断状态，快速失败
        HALF_OPEN   // 半开状态，尝试恢复
    }

    // --- 监听器接口 ---
    @FunctionalInterface
    public interface StateChangeListener {
        void onStateChange(String name, State from, State to);
    }

    // --- 熔断异常 ---
    @Getter
    public static class CircuitBreakerOpenException extends RuntimeException {
        private final String circuitBreakerName;
        private final Duration remainingTimeout;

        public CircuitBreakerOpenException(String name, Duration remainingTimeout) {
            super(String.format("Circuit breaker [%s] is OPEN, remaining timeout: %dms",
                    name, remainingTimeout.toMillis()));
            this.circuitBreakerName = name;
            this.remainingTimeout = remainingTimeout;
        }
    }

    // --- Builder ---
    public static class Builder {
        private String name = "default";
        private int failureThreshold = 5;
        private Duration timeout = Duration.ofSeconds(30);
        private int halfOpenRequests = 3;
        private StateChangeListener listener;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder failureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder halfOpenRequests(int requests) {
            this.halfOpenRequests = requests;
            return this;
        }

        public Builder onStateChange(StateChangeListener listener) {
            this.listener = listener;
            return this;
        }

        public Builder onStateChange(Consumer<State> handler) {
            this.listener = (name, from, to) -> handler.accept(to);
            return this;
        }

        public CircuitBreaker build() {
            return new CircuitBreaker(name, failureThreshold, timeout, halfOpenRequests, listener);
        }
    }
}