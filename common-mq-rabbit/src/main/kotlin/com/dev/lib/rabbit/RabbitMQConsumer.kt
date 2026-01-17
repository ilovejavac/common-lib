package com.dev.lib.rabbit

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.consumer.MQConsumer
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.rabbitmq.client.Channel
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
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
        // 动态注册 listener 实现较复杂，这里提供模板方法
        // 使用者在实际使用时使用 @RabbitListener 注解 + RabbitMQHandlerHelper
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

        try {
            val action = handler(message)
            when (action) {
                AckAction.ACK -> {
                    channel?.basicAck(deliveryTag ?: 0, false)
                    retryCountCache.invalidate(msgId)
                }
                AckAction.NACK -> {
                    if (currentRetry.get() < message.retry) {
                        currentRetry.incrementAndGet()
                        channel?.basicNack(deliveryTag ?: 0, false, true)
                    } else {
                        channel?.basicReject(deliveryTag ?: 0, false)
                        retryCountCache.invalidate(msgId)
                    }
                }
                AckAction.REJECT -> {
                    channel?.basicReject(deliveryTag ?: 0, false)
                    retryCountCache.invalidate(msgId)
                }
            }
        } catch (e: Exception) {
            if (currentRetry.get() < message.retry) {
                currentRetry.incrementAndGet()
                channel?.basicNack(deliveryTag ?: 0, false, true)
            } else {
                channel?.basicReject(deliveryTag ?: 0, false)
                retryCountCache.invalidate(msgId)
            }
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
