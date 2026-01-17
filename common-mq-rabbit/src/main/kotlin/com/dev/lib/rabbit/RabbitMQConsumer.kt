package com.dev.lib.rabbit

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.consumer.MQConsumer
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.rabbitmq.client.Channel
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Component
class RabbitMQConsumer : MQConsumer {

    override fun <T> listen(destination: String, handler: (T) -> AckAction) {
        listen(destination, "", 1, handler)
    }

    override fun <T> listen(
        destination: String,
        group: String,
        concurrency: Int,
        handler: (T) -> AckAction
    ) {
    }
}

object RabbitMQHandlerHelper {

    private val retryCountCache: Cache<String, AtomicInteger> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .build()

    fun <T> handleWithRetry(
        message: MessageExtend<T>,
        channel: Channel?,
        deliveryTag: Long?,
        handler: (MessageExtend<T>) -> AckAction
    ) {
        val msgId = message.id.toString()
        val currentRetry = retryCountCache.get(msgId) { AtomicInteger(0) }!!

        val action = try {
            handler(message)
        } catch (e: Exception) {
            handleRetryOrReject(message, channel, deliveryTag, currentRetry, msgId)
            return
        }

        when (action) {
            AckAction.ACK -> {
                channel?.basicAck(deliveryTag ?: 0, false)
                retryCountCache.invalidate(msgId)
            }
            AckAction.NACK -> handleNack(message, channel, deliveryTag, currentRetry, msgId)
            AckAction.REJECT -> {
                channel?.basicReject(deliveryTag ?: 0, false)
                retryCountCache.invalidate(msgId)
            }
        }
    }

    private fun <T> handleRetryOrReject(
        message: MessageExtend<T>,
        channel: Channel?,
        deliveryTag: Long?,
        currentRetry: AtomicInteger,
        msgId: String
    ) {
        if (currentRetry.get() < message.retry) {
            currentRetry.incrementAndGet()
            channel?.basicNack(deliveryTag ?: 0, false, true)
        } else {
            channel?.basicReject(deliveryTag ?: 0, false)
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
            channel?.basicNack(deliveryTag ?: 0, false, true)
        } else {
            channel?.basicReject(deliveryTag ?: 0, false)
            retryCountCache.invalidate(msgId)
        }
    }

    fun <T> handle(
        message: T,
        channel: Channel?,
        deliveryTag: Long?,
        handler: (T) -> AckAction
    ) {
        val action = handler(message)
        if (channel != null && deliveryTag != null) {
            when (action) {
                AckAction.ACK -> channel.basicAck(deliveryTag, false)
                AckAction.NACK -> channel.basicNack(deliveryTag, false, true)
                AckAction.REJECT -> channel.basicReject(deliveryTag, false)
            }
        }
    }
}
