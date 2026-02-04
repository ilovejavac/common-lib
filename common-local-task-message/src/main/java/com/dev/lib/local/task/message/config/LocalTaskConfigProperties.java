package com.dev.lib.local.task.message.config;

import com.dev.lib.local.task.message.poller.strategy.BackoffStrategy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地任务消息配置属性
 * 支持按任务类型配置不同的轮询参数
 */
public class LocalTaskConfigProperties {

    /**
     * 任务类型配置列表（新）
     */
    private List<TaskTypeConfig> typeConfigs = new ArrayList<>();

    /**
     * 全局默认配置
     */
    private GlobalConfig global = new GlobalConfig();

    /**
     * 任务组配置（兼容旧版本）
     * @deprecated 使用 typeConfigs 替代
     */
    @Deprecated
    private List<TaskGroupConfig> groupConfigs;

    // 手动添加 getter/setter 方法
    public List<TaskTypeConfig> getTypeConfigs() {
        return typeConfigs;
    }

    public void setTypeConfigs(List<TaskTypeConfig> typeConfigs) {
        this.typeConfigs = typeConfigs;
    }

    public GlobalConfig getGlobal() {
        return global;
    }

    public void setGlobal(GlobalConfig global) {
        this.global = global;
    }

    public List<TaskGroupConfig> getGroupConfigs() {
        return groupConfigs;
    }

    public void setGroupConfigs(List<TaskGroupConfig> groupConfigs) {
        this.groupConfigs = groupConfigs;
    }

    /**
     * 任务类型配置
     */
    public static class TaskTypeConfig {

        /**
         * 任务类型（如 HTTP_NOTIFY, MQ_NOTIFY, RABBIT_RETRY 等）
         */
        private String taskType;

        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * 门牌号数量（用于并发执行）
         * 如 5 表示有 5 个门牌号（0-4）
         */
        private int houseNumberCount = 1;

        /**
         * 轮询间隔（如 10s, 30s, 1m）
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
         * 退避策略（FIXED, LINEAR, EXPONENTIAL）
         */
        private BackoffStrategy backoffStrategy = BackoffStrategy.LINEAR;

        // 手动添加 getter/setter 方法
        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getHouseNumberCount() {
            return houseNumberCount;
        }

        public void setHouseNumberCount(int houseNumberCount) {
            this.houseNumberCount = houseNumberCount;
        }

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public int getFetchLimit() {
            return fetchLimit;
        }

        public void setFetchLimit(int fetchLimit) {
            this.fetchLimit = fetchLimit;
        }

        public int getMaxRetry() {
            return maxRetry;
        }

        public void setMaxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
        }

        public Duration getBaseDelay() {
            return baseDelay;
        }

        public void setBaseDelay(Duration baseDelay) {
            this.baseDelay = baseDelay;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }

        public BackoffStrategy getBackoffStrategy() {
            return backoffStrategy;
        }

        public void setBackoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
        }

    }

    /**
     * 全局默认配置
     */
    public static class GlobalConfig {

        /**
         * 默认轮询间隔
         */
        private Duration pollInterval = Duration.ofSeconds(60);

        /**
         * 默认最大重试次数
         */
        private int maxRetry = 30;

        /**
         * 默认基础延迟
         */
        private Duration baseDelay = Duration.ofSeconds(1);

        /**
         * 默认最大延迟
         */
        private Duration maxDelay = Duration.ofMinutes(5);

        /**
         * 默认退避策略
         */
        private BackoffStrategy backoffStrategy = BackoffStrategy.LINEAR;

        // 手动添加 getter/setter 方法
        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public int getMaxRetry() {
            return maxRetry;
        }

        public void setMaxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
        }

        public Duration getBaseDelay() {
            return baseDelay;
        }

        public void setBaseDelay(Duration baseDelay) {
            this.baseDelay = baseDelay;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }

        public BackoffStrategy getBackoffStrategy() {
            return backoffStrategy;
        }

        public void setBackoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
        }

    }

    /**
     * 任务组配置（兼容旧版本）
     * @deprecated 使用 TaskTypeConfig 替代
     */
    @Deprecated
    public static class TaskGroupConfig {

        private String group = "default";

        private List<Integer> houseNumbers = new ArrayList<>();

        private String cron;

        private Long fixedDelayMs = 0L;

        private Integer limit = 100;

    }

}
