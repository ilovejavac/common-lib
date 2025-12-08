package com.dev.lib.util.breaker;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 断路器注册表
 * 用于集中管理多个断路器实例
 */
@Slf4j
public class CircuitBreakerRegistry {

    private final Map<String, CircuitBreaker> registry = new ConcurrentHashMap<>();
    private final CircuitBreaker.Builder defaultConfig;

    public CircuitBreakerRegistry() {
        this.defaultConfig = CircuitBreaker.builder()
                .failureThreshold(5)
                .timeout(Duration.ofSeconds(30))
                .halfOpenRequests(3);
    }

    public CircuitBreakerRegistry(CircuitBreaker.Builder defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    /**
     * 获取或创建断路器
     */
    public CircuitBreaker get(String name) {
        return registry.computeIfAbsent(name, n ->
                defaultConfig.name(n).build()
        );
    }

    /**
     * 使用自定义配置创建断路器
     */
    public CircuitBreaker create(String name, CircuitBreaker.Builder config) {
        CircuitBreaker breaker = config.name(name).build();
        registry.put(name, breaker);
        return breaker;
    }

    /**
     * 获取断路器（可能不存在）
     */
    public Optional<CircuitBreaker> find(String name) {
        return Optional.ofNullable(registry.get(name));
    }

    /**
     * 移除断路器
     */
    public void remove(String name) {
        registry.remove(name);
    }

    /**
     * 重置所有断路器
     */
    public void resetAll() {
        registry.values().forEach(CircuitBreaker::forceReset);
    }

    /**
     * 获取所有断路器状态
     */
    public Map<String, CircuitBreaker.State> getAllStates() {
        Map<String, CircuitBreaker.State> states = new ConcurrentHashMap<>();
        registry.forEach((name, breaker) -> states.put(name, breaker.getState()));
        return states;
    }

    /**
     * 装饰一个 Supplier
     */
    public <T> Supplier<T> decorateSupplier(String name, Supplier<T> supplier) {
        CircuitBreaker breaker = get(name);
        return () -> breaker.execute(supplier::get);
    }

    /**
     * 装饰一个 Runnable
     */
    public Runnable decorateRunnable(String name, Runnable runnable) {
        CircuitBreaker breaker = get(name);
        return () -> breaker.executeVoid(runnable);
    }
}