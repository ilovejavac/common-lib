package com.dev.lib.kafka

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.local.task.message.poller.core.PollerEngineRegistry
import com.dev.lib.mq.AckCallback
import com.dev.lib.mq.MQTemplate
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.reliability.ReliabilityConfig
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import java.util.concurrent.CompletableFuture

class KafkaMQTemplate(
    private val template: KafkaTemplate<String, Any>,
    private val pollerRegistry: PollerEngineRegistry?
) : MQTemplate {

    private var reliabilityConfig: ReliabilityConfig = ReliabilityConfig.DEFAULT

    override fun setReliabilityConfig(config: ReliabilityConfig) {
        this.reliabilityConfig = config
    }

    override fun <T> send(destination: String, message: MessageExtend<T>) {
        try {
            template.send(destination, message.key, buildMessage(message)).get()
        } catch (e: Exception) {
            savePendingIfNeeded(message, destination)
            throw e
        }
    }

    override fun <T> sendAsync(
        destination: String,
        message: MessageExtend<T>,
        ack: AckCallback<T>
    ) {
        val future: CompletableFuture<*> = template.send(destination, message.key, buildMessage(message))
        future.whenComplete { _, throwable ->
            if (throwable == null) {
                ack.onSuccess(message)
            } else {
                savePendingIfNeeded(message, destination)
                ack.onFailure(message, throwable)
            }
        }
    }

    override fun <T> convertAndSend(destination: String, message: MessageExtend<T>) {
        template.send(destination, message.key, buildMessage(message))
    }

    private fun <T> buildMessage(message: MessageExtend<T>): Message<MessageExtend<T>> {
        val builder = MessageBuilder.withPayload(message)

        message.headers.forEach { (k, v) -> builder.setHeader(k, v) }

        message.delay?.let { builder.setHeader("DELAY_MS", it) }
        message.shardingKey?.let { builder.setHeader("PARTITION_KEY", it) }
        message.priority?.let { builder.setHeader("PRIORITY", it) }
        message.ttl?.let { builder.setHeader("TTL_MS", it) }
        message.deadLetter?.let { builder.setHeader("DLQ_TOPIC", it) }
        builder.setHeader("RETRY", message.retry)
        builder.setHeader("RETRY_DELAY_MS", message.retryDelay)

        return builder.build()
    }

    private fun <T> savePendingIfNeeded(message: MessageExtend<T>, destination: String) {
        if (pollerRegistry != null) {
            CoroutineScopeHolder.launch {
                val payload = mapOf(
                    "destination" to destination,
                    "key" to message.key,
                    "body" to message.body,
                    "headers" to message.headers
                )
                pollerRegistry.submit("KAFKA_RETRY", message.id, payload)
            }
        }
    }
}
