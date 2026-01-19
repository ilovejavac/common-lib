package com.dev.lib.rabbit

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.consumer.MQConsumer
import com.dev.lib.mq.consumer.RetryHandler
import com.rabbitmq.client.Channel
import org.springframework.stereotype.Component

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

    fun <T> handleWithRetry(
        message: MessageExtend<T>,
        channel: Channel?,
        deliveryTag: Long?,
        handler: (MessageExtend<T>) -> AckAction
    ) {
        val action = RetryHandler.handleWithRetry(message, handler)
        applyAck(action, channel, deliveryTag, message.id.toString())
    }

    private fun applyAck(action: AckAction, channel: Channel?, deliveryTag: Long?, msgId: String) {
        if (channel == null || deliveryTag == null) return

        when (action) {
            AckAction.ACK -> channel.basicAck(deliveryTag, false)
            AckAction.NACK -> channel.basicNack(deliveryTag, false, true)
            AckAction.REJECT -> channel.basicReject(deliveryTag, false)
        }
    }

    fun <T> handle(message: T, channel: Channel?, deliveryTag: Long?, handler: (T) -> AckAction) {
        val action = RetryHandler.handle(message, handler)
        if (channel != null && deliveryTag != null) {
            when (action) {
                AckAction.ACK -> channel.basicAck(deliveryTag, false)
                AckAction.NACK -> channel.basicNack(deliveryTag, false, true)
                AckAction.REJECT -> channel.basicReject(deliveryTag, false)
            }
        }
    }
}
