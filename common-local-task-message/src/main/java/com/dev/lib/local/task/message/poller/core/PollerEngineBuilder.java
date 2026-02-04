package com.dev.lib.local.task.message.poller.core;

import java.util.List;

/**
 * Poller 引擎构建器
 * 用于简化 PollerEngine 的创建和配置
 */
public class PollerEngineBuilder {

    private PollerConfig config = PollerConfig.builder().build();
    private PollerStorage storage;
    private PollerTaskExecutor executor;

    /**
     * 设置配置
     */
    public PollerEngineBuilder config(PollerConfig config) {
        this.config = config;
        return this;
    }

    /**
     * 设置任务类型
     */
    public PollerEngineBuilder taskType(String taskType) {
        config = PollerConfig.builder()
                .taskType(taskType)
                .houseNumbers(config.getHouseNumbers())
                .pollInterval(config.getPollInterval())
                .fetchLimit(config.getFetchLimit())
                .maxRetry(config.getMaxRetry())
                .baseDelay(config.getBaseDelay())
                .maxDelay(config.getMaxDelay())
                .backoffStrategy(config.getBackoffStrategy())
                .build();
        return this;
    }

    /**
     * 设置门牌号数量（从 0 到 count-1）
     */
    public PollerEngineBuilder houseNumbers(int count) {
        List<Integer> numbers = java.util.stream.IntStream.range(0, count)
                .boxed()
                .toList();
        config = PollerConfig.builder()
                .taskType(config.getTaskType())
                .houseNumbers(numbers)
                .pollInterval(config.getPollInterval())
                .fetchLimit(config.getFetchLimit())
                .maxRetry(config.getMaxRetry())
                .baseDelay(config.getBaseDelay())
                .maxDelay(config.getMaxDelay())
                .backoffStrategy(config.getBackoffStrategy())
                .build();
        return this;
    }

    /**
     * 设置轮询间隔
     */
    public PollerEngineBuilder pollInterval(java.time.Duration interval) {
        config = PollerConfig.builder()
                .taskType(config.getTaskType())
                .houseNumbers(config.getHouseNumbers())
                .pollInterval(interval)
                .fetchLimit(config.getFetchLimit())
                .maxRetry(config.getMaxRetry())
                .baseDelay(config.getBaseDelay())
                .maxDelay(config.getMaxDelay())
                .backoffStrategy(config.getBackoffStrategy())
                .build();
        return this;
    }

    /**
     * 设置最大重试次数
     */
    public PollerEngineBuilder maxRetry(int maxRetry) {
        config = PollerConfig.builder()
                .taskType(config.getTaskType())
                .houseNumbers(config.getHouseNumbers())
                .pollInterval(config.getPollInterval())
                .fetchLimit(config.getFetchLimit())
                .maxRetry(maxRetry)
                .baseDelay(config.getBaseDelay())
                .maxDelay(config.getMaxDelay())
                .backoffStrategy(config.getBackoffStrategy())
                .build();
        return this;
    }

    /**
     * 设置退避策略
     */
    public PollerEngineBuilder backoffStrategy(com.dev.lib.local.task.message.poller.strategy.BackoffStrategy strategy) {
        config = PollerConfig.builder()
                .taskType(config.getTaskType())
                .houseNumbers(config.getHouseNumbers())
                .pollInterval(config.getPollInterval())
                .fetchLimit(config.getFetchLimit())
                .maxRetry(config.getMaxRetry())
                .baseDelay(config.getBaseDelay())
                .maxDelay(config.getMaxDelay())
                .backoffStrategy(strategy)
                .build();
        return this;
    }

    /**
     * 设置基础延迟
     */
    public PollerEngineBuilder baseDelay(java.time.Duration delay) {
        config = PollerConfig.builder()
                .taskType(config.getTaskType())
                .houseNumbers(config.getHouseNumbers())
                .pollInterval(config.getPollInterval())
                .fetchLimit(config.getFetchLimit())
                .maxRetry(config.getMaxRetry())
                .baseDelay(delay)
                .maxDelay(config.getMaxDelay())
                .backoffStrategy(config.getBackoffStrategy())
                .build();
        return this;
    }

    /**
     * 设置最大延迟
     */
    public PollerEngineBuilder maxDelay(java.time.Duration delay) {
        config = PollerConfig.builder()
                .taskType(config.getTaskType())
                .houseNumbers(config.getHouseNumbers())
                .pollInterval(config.getPollInterval())
                .fetchLimit(config.getFetchLimit())
                .maxRetry(config.getMaxRetry())
                .baseDelay(config.getBaseDelay())
                .maxDelay(delay)
                .backoffStrategy(config.getBackoffStrategy())
                .build();
        return this;
    }

    /**
     * 设置每次从数据库获取的最大任务数
     */
    public PollerEngineBuilder fetchLimit(int fetchLimit) {
        config = PollerConfig.builder()
                .taskType(config.getTaskType())
                .houseNumbers(config.getHouseNumbers())
                .pollInterval(config.getPollInterval())
                .fetchLimit(fetchLimit)
                .maxRetry(config.getMaxRetry())
                .baseDelay(config.getBaseDelay())
                .maxDelay(config.getMaxDelay())
                .backoffStrategy(config.getBackoffStrategy())
                .build();
        return this;
    }

    /**
     * 设置存储
     */
    public PollerEngineBuilder storage(PollerStorage storage) {
        this.storage = storage;
        return this;
    }

    /**
     * 设置任务执行器
     */
    public PollerEngineBuilder executor(PollerTaskExecutor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * 构建 PollerEngine
     */
    public PollerEngine build() {
        if (storage == null) {
            throw new IllegalArgumentException("storage is required");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor is required");
        }

        if (config.getTaskType() == null) {
            throw new IllegalArgumentException("taskType is required");
        }

        return new PollerEngineImpl(config, storage, executor);
    }

    /**
     * 创建新的构建器
     */
    public static PollerEngineBuilder builder() {
        return new PollerEngineBuilder();
    }

}
