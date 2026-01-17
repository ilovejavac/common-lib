package com.dev.lib.rabbit

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.consumer.MQConsumer
import com.rabbitmq.client.Channel
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
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

    private val retryCountMap = ConcurrentHashMap<String, AtomicInteger>()

    fun <T> handleWithRetry(
        message: MessageExtend<T>,
        channel: Channel?,
        deliveryTag: Long?,
        handler: (MessageExtend<T>) -> AckAction
    ) {
        val msgId = message.id.toString()
        val currentRetry = retryCountMap.getOrPut(msgId) { AtomicInteger(0) }

        try {
            val action = handler(message)
            when (action) {
                AckAction.ACK -> {
                    channel?.basicAck(deliveryTag ?: 0, false)
                    retryCountMap.remove(msgId)
                }
                AckAction.NACK -> {
                    if (currentRetry.get() < message.retry) {
                        currentRetry.incrementAndGet()
                        channel?.basicNack(deliveryTag ?: 0, false, true)
                        Thread.sleep(message.retryDelay)
                    } else {
                        channel?.basicReject(deliveryTag ?: 0, false)
                        retryCountMap.remove(msgId)
                    }
                }
                AckAction.REJECT -> {
                    channel?.basicReject(deliveryTag ?: 0, false)
                    retryCountMap.remove(msgId)
                }
            }
        } catch (e: Exception) {
            if (currentRetry.get() < message.retry) {
                currentRetry.incrementAndGet()
                channel?.basicNack(deliveryTag ?: 0, false, true)
                Thread.sleep(message.retryDelay)
            } else {
                channel?.basicReject(deliveryTag ?: 0, false)
                retryCountMap.remove(msgId)
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
