package com.dev.lib.kafka

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.local.task.message.storage.LocalTaskMessageStorage
import com.dev.lib.mq.AbstractMQTemplate
import com.dev.lib.mq.AckCallback
import com.dev.lib.mq.MessageExtend
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import java.util.concurrent.CompletableFuture

class KafkaMQTemplate(
    private val template: KafkaTemplate<String, Any>,
    private val messageStorage: LocalTaskMessageStorage?
) : AbstractMQTemplate() {

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

        // 自定义 headers
        message.headers.forEach { (k, v) -> builder.setHeader(k, v) }

        // 延迟消息（Kafka 不原生支持，存入 header 由消费端或时间轮处理）
        message.delay?.let { builder.setHeader("DELAY_MS", it) }

        // 分区键（用于消息路由到同一分区，确保顺序消费）
        message.shardingKey?.let { builder.setHeader("PARTITION_KEY", it) }

        // 优先级（自定义，需要消费端配合）
        message.priority?.let { builder.setHeader("PRIORITY", it) }

        // TTL（消息过期时间）
        message.ttl?.let { builder.setHeader("TTL_MS", it) }

        // 死信队列
        message.deadLetter?.let { builder.setHeader("DLQ_TOPIC", it) }

        // 重试次数
        builder.setHeader("RETRY", message.retry)

        // 重试延迟
        builder.setHeader("RETRY_DELAY_MS", message.retryDelay)

        return builder.build()
    }

    private fun <T> savePendingIfNeeded(message: MessageExtend<T>, destination: String) {
        if (messageStorage != null) {
            CoroutineScopeHolder.launch {
                messageStorage.saveAsPending(message, destination)
            }
        }
    }
}
