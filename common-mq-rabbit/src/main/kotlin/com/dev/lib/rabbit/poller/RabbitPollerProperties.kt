package com.dev.lib.rabbit.poller

import com.dev.lib.local.task.message.poller.strategy.BackoffStrategy
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * RabbitMQ Poller 配置属性
 * 配置 RabbitMQ 消息重试任务的轮询参数
 */
@ConfigurationProperties(prefix = "app.rabbit.poller")
data class RabbitPollerProperties(
    /**
     * 门牌号数量（用于并发执行）
     * 默认 5 表示 5 个并发线程，门牌号为 0-4
     */
    val houseNumberCount: Int = 5,

    /**
     * 轮询间隔
     */
    val pollInterval: Duration = Duration.ofSeconds(5),

    /**
     * 每次从数据库获取的最大任务数
     */
    val fetchLimit: Int = 100,

    /**
     * 最大重试次数
     */
    val maxRetry: Int = 5,

    /**
     * 基础延迟时间
     */
    val baseDelay: Duration = Duration.ofSeconds(2),

    /**
     * 最大延迟时间
     */
    val maxDelay: Duration = Duration.ofMinutes(10),

    /**
     * 退避策略
     */
    val backoffStrategy: BackoffStrategy = BackoffStrategy.LINEAR
)
