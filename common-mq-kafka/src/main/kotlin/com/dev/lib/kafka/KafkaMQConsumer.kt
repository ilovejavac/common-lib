package com.dev.lib.kafka

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.consumer.MQConsumer
import com.dev.lib.mq.consumer.RetryHandler
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.AcknowledgingConsumerAwareMessageListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KafkaMQConsumer : MQConsumer {

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

object KafkaMQHandlerHelper {

    fun <T> handleWithRetry(
        record: ConsumerRecord<String, Any>,
        acknowledgment: Acknowledgment?,
        consumer: Consumer<*, *>?,
        handler: (MessageExtend<T>) -> AckAction
    ) {
        @Suppress("UNCHECKED_CAST")
        val message = record.value() as? MessageExtend<T> ?: return

        val action = RetryHandler.handleWithRetry(message, handler)
        applyAck(action, acknowledgment)
    }

    fun <T> handle(
        record: ConsumerRecord<String, Any>,
        acknowledgment: Acknowledgment?,
        consumer: Consumer<*, *>?,
        handler: (T) -> AckAction
    ) {
        @Suppress("UNCHECKED_CAST")
        val message = record.value() as? T ?: return

        val action = RetryHandler.handle(message, handler)
        applyAck(action, acknowledgment)
    }

    private fun applyAck(action: AckAction, acknowledgment: Acknowledgment?) {
        when (action) {
            // Kafka 只有 acknowledge()，重试由 RetryHandler 控制
            AckAction.ACK -> acknowledgment?.acknowledge()
            AckAction.NACK -> acknowledgment?.acknowledge()
            // REJECT: 跳过消息，不确认（可能导致重复消费，但不会丢失）
            AckAction.REJECT -> {}
        }
    }
}
