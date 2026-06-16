package com.dev.lib.rabbit

import com.dev.lib.mq.AckCallback
import com.dev.lib.mq.MQTemplate
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.reliability.ReliabilityConfig
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate

class RabbitMQTemplate(
    private val template: RabbitTemplate
) : MQTemplate {

    private var reliabilityConfig: ReliabilityConfig = ReliabilityConfig.DEFAULT

    override fun setReliabilityConfig(config: ReliabilityConfig) {
        this.reliabilityConfig = config
    }

    override fun <T> send(destination: String, message: MessageExtend<T>) {
        val correlationData = createCorrelationData(message)
        template.convertAndSend(destination, message.key, message, createPostProcessor(message), correlationData)
    }

    override fun <T> sendAsync(
        destination: String,
        message: MessageExtend<T>,
        ack: AckCallback<T>
    ) {
        val correlationData = CorrelationData(message.id.toString())
        correlationData.future.whenComplete { _, throwable ->
            if (throwable == null) {
                ack.onSuccess(message)
            } else {
                ack.onFailure(message, throwable)
            }
        }
        template.convertAndSend(destination, message.key, message, createPostProcessor(message), correlationData)
    }

    override fun <T> convertAndSend(destination: String, message: MessageExtend<T>) {
        template.convertAndSend(destination, message.key, message, createPostProcessor(message))
    }

    private fun <T> createCorrelationData(message: MessageExtend<T>): CorrelationData = CorrelationData(message.id)

    private fun <T> createPostProcessor(message: MessageExtend<T>): MessagePostProcessor {
        return MessagePostProcessor { msg ->
            val props = msg.messageProperties

            if (message.persistent) {
                props.deliveryMode = MessageDeliveryMode.PERSISTENT
            }

            message.headers.forEach { (k, v) -> props.headers[k] = v }

            message.ttl?.let { props.setExpiration(it.toString()) }
            message.priority?.let { props.priority = it }
            message.delay?.let { props.headers["x-delay"] = it }
            message.deadLetter?.let {
                props.headers["x-dead-letter-exchange"] = ""
                props.headers["x-dead-letter-routing-key"] = it
            }
            message.shardingKey?.let { props.headers["x-sharding-key"] = it }

            msg
        }
    }
}
