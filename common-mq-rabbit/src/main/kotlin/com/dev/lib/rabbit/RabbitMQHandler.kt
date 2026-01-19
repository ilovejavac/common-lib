package com.dev.lib.rabbit

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend
import com.rabbitmq.client.Channel
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * RabbitMQ 消费端辅助工具
 */
object RabbitMQHandler {

    private val retryCountCache: Cache<String, AtomicInteger> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100_000)
        .build()

    /**
     * 带重试的消息处理
     */
    fun <T> handleWithRetry(
        message: MessageExtend<T>,
        channel: Channel?,
        deliveryTag: Long?,
        handler: (MessageExtend<T>) -> AckAction
    ) {
        val msgId = message.id.toString()
        val currentRetry = retryCountCache.get(msgId) { AtomicInteger(0) }

        val action = try {
            handler(message)
        } catch (e: Exception) {
            handleRetryOrReject(message, currentRetry, msgId)
            return
        }

        when (action) {
            AckAction.ACK -> {
                applyAck(AckAction.ACK, channel, deliveryTag)
                retryCountCache.invalidate(msgId)
            }
            AckAction.NACK -> handleNack(message, channel, deliveryTag, currentRetry, msgId)
            AckAction.REJECT -> {
                applyAck(AckAction.REJECT, channel, deliveryTag)
                retryCountCache.invalidate(msgId)
            }
        }
    }

    /**
     * 不带重试的消息处理
     */
    fun <T> handle(
        message: T,
        channel: Channel?,
        deliveryTag: Long?,
        handler: (T) -> AckAction
    ) {
        val action = try {
            handler(message)
        } catch (e: Exception) {
            AckAction.REJECT
        }
        applyAck(action, channel, deliveryTag)
    }

    /**
     * 根据 AckAction 执行对应的 ACK 操作
     */
    fun applyAck(action: AckAction, channel: Channel?, deliveryTag: Long?) {
        if (channel == null || deliveryTag == null) return

        when (action) {
            AckAction.ACK -> channel.basicAck(deliveryTag, false)
            AckAction.NACK -> channel.basicNack(deliveryTag, false, true)
            AckAction.REJECT -> channel.basicReject(deliveryTag, false)
        }
    }

    private fun <T> handleRetryOrReject(
        message: MessageExtend<T>,
        currentRetry: AtomicInteger,
        msgId: String
    ) {
        if (currentRetry.get() < message.retry) {
            currentRetry.incrementAndGet()
        } else {
            retryCountCache.invalidate(msgId)
        }
    }

    private fun <T> handleNack(
        message: MessageExtend<T>,
        channel: Channel?,
        deliveryTag: Long?,
        currentRetry: AtomicInteger,
        msgId: String
    ) {
        if (currentRetry.get() < message.retry) {
            currentRetry.incrementAndGet()
            applyAck(AckAction.NACK, channel, deliveryTag)
        } else {
            applyAck(AckAction.REJECT, channel, deliveryTag)
            retryCountCache.invalidate(msgId)
        }
    }
}
