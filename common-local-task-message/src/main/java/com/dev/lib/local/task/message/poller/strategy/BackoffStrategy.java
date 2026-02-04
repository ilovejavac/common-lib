package com.dev.lib.local.task.message.poller.strategy;

import java.time.Duration;

/**
 * 退避策略枚举
 */
public enum BackoffStrategy {

    /**
     * 固定延迟 - 每次重试间隔相同
     */
    FIXED {
        @Override
        public long calculateDelay(int retryCount, Duration baseDelay, Duration maxDelay) {
            return baseDelay.toMillis();
        }
    },

    /**
     * 线性延迟 - 延迟随重试次数线性增长
     */
    LINEAR {
        @Override
        public long calculateDelay(int retryCount, Duration baseDelay, Duration maxDelay) {
            long delay = baseDelay.toMillis() * (retryCount + 1);
            return Math.min(delay, maxDelay.toMillis());
        }
    },

    /**
     * 指数延迟 - 延迟随重试次数指数增长
     */
    EXPONENTIAL {
        @Override
        public long calculateDelay(int retryCount, Duration baseDelay, Duration maxDelay) {
            long delay = baseDelay.toMillis() * (1L << Math.min(retryCount, 31));
            return Math.min(delay, maxDelay.toMillis());
        }
    };

    /**
     * 计算延迟时间
     *
     * @param retryCount 当前重试次数
     * @param baseDelay  基础延迟
     * @param maxDelay   最大延迟
     * @return 延迟毫秒数
     */
    public abstract long calculateDelay(int retryCount, Duration baseDelay, Duration maxDelay);

}
