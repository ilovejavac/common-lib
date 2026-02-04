package com.dev.lib.local.task.message.poller.core;

import com.dev.lib.local.task.message.poller.strategy.BackoffStrategy;

import java.time.Duration;

/**
 * Poller 配置接口
 * 各业务模块实现此接口以提供任务类型的配置
 */
public interface PollerConfiguration {

    /**
     * 获取任务类型
     */
    String getTaskType();

    /**
     * 获取门牌号数量
     */
    default int getHouseNumberCount() {
        return 5;
    }

    /**
     * 获取轮询间隔
     */
    default Duration getPollInterval() {
        return Duration.ofSeconds(10);
    }

    /**
     * 获取每次从数据库获取的最大任务数
     */
    default int getFetchLimit() {
        return 100;
    }

    /**
     * 获取最大重试次数
     */
    default int getMaxRetry() {
        return 3;
    }

    /**
     * 获取基础延迟时间
     */
    default Duration getBaseDelay() {
        return Duration.ofSeconds(1);
    }

    /**
     * 获取最大延迟时间
     */
    default Duration getMaxDelay() {
        return Duration.ofMinutes(5);
    }

    /**
     * 获取退避策略
     */
    default BackoffStrategy getBackoffStrategy() {
        return BackoffStrategy.LINEAR;
    }

}
