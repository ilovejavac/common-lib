package com.dev.lib.local.task.message.poller.core;

import com.dev.lib.local.task.message.poller.strategy.BackoffStrategy;

import java.time.Duration;
import java.util.List;

/**
 * Poller 轮询配置
 * 用于配置单个任务类型的轮询参数
 */
public class PollerConfig {

    /**
     * 任务类型（如 HTTP_NOTIFY, MQ_NOTIFY, RABBIT_RETRY 等）
     */
    private String taskType;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 门牌号列表（用于并发执行）
     * 如 [0, 1, 2, 3, 4] 表示有 5 个门牌号，可以并发执行
     */
    private List<Integer> houseNumbers = List.of(0);

    /**
     * 轮询间隔
     */
    private Duration pollInterval = Duration.ofSeconds(10);

    /**
     * 每次从数据库获取的最大任务数
     */
    private int fetchLimit = 100;

    /**
     * 最大重试次数
     */
    private int maxRetry = 3;

    /**
     * 基础延迟时间（用于退避策略）
     */
    private Duration baseDelay = Duration.ofSeconds(1);

    /**
     * 最大延迟时间（用于退避策略）
     */
    private Duration maxDelay = Duration.ofMinutes(5);

    /**
     * 退避策略
     */
    private BackoffStrategy backoffStrategy = BackoffStrategy.LINEAR;

    public PollerConfig() {
    }

    public PollerConfig(String taskType, boolean enabled, List<Integer> houseNumbers, Duration pollInterval,
                        int fetchLimit, int maxRetry, Duration baseDelay, Duration maxDelay, BackoffStrategy backoffStrategy) {
        this.taskType = taskType;
        this.enabled = enabled;
        this.houseNumbers = houseNumbers;
        this.pollInterval = pollInterval;
        this.fetchLimit = fetchLimit;
        this.maxRetry = maxRetry;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
        this.backoffStrategy = backoffStrategy;
    }

    // 手动添加 builder 方法
    public static PollerConfigBuilder builder() {
        return new PollerConfigBuilder();
    }

    public static class PollerConfigBuilder {
        private String taskType;
        private boolean enabled = true;
        private List<Integer> houseNumbers = List.of(0);
        private Duration pollInterval = Duration.ofSeconds(10);
        private int fetchLimit = 100;
        private int maxRetry = 3;
        private Duration baseDelay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofMinutes(5);
        private BackoffStrategy backoffStrategy = BackoffStrategy.LINEAR;

        public PollerConfigBuilder taskType(String taskType) {
            this.taskType = taskType;
            return this;
        }

        public PollerConfigBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public PollerConfigBuilder houseNumbers(List<Integer> houseNumbers) {
            this.houseNumbers = houseNumbers;
            return this;
        }

        public PollerConfigBuilder pollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        public PollerConfigBuilder fetchLimit(int fetchLimit) {
            this.fetchLimit = fetchLimit;
            return this;
        }

        public PollerConfigBuilder maxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
            return this;
        }

        public PollerConfigBuilder baseDelay(Duration baseDelay) {
            this.baseDelay = baseDelay;
            return this;
        }

        public PollerConfigBuilder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public PollerConfigBuilder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        public PollerConfig build() {
            return new PollerConfig(
                taskType, enabled, houseNumbers, pollInterval,
                fetchLimit, maxRetry, baseDelay, maxDelay, backoffStrategy
            );
        }
    }

    // 手动添加 getter 方法
    public String getTaskType() {
        return taskType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<Integer> getHouseNumbers() {
        return houseNumbers;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public int getFetchLimit() {
        return fetchLimit;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public Duration getBaseDelay() {
        return baseDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public BackoffStrategy getBackoffStrategy() {
        return backoffStrategy;
    }

}
