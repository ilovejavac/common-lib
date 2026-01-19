package com.dev.lib.rocketmq

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.local.task.message.storage.LocalTaskMessageStorage
import com.dev.lib.mq.AbstractMQTemplate
import com.dev.lib.mq.AckCallback
import com.dev.lib.mq.MessageExtend
import org.apache.rocketmq.client.producer.SendCallback
import org.apache.rocketmq.client.producer.SendResult
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.apache.rocketmq.spring.support.RocketMQHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder

class RocketMQTemplate(
    private val template: RocketMQTemplate,
    private val messageStorage: LocalTaskMessageStorage?
) : AbstractMQTemplate() {

    override fun <T> send(destination: String, message: MessageExtend<T>) {
        try {
            template.syncSend(destination, buildMessage(message))
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
        try {
            template.asyncSend(destination, buildMessage(message), object : SendCallback {
                override fun onSuccess(result: SendResult?) {
                    ack.onSuccess(message)
                }

                override fun onException(e: Throwable?) {
                    savePendingIfNeeded(message, destination)
                    ack.onFailure(message, e ?: Exception("Unknown error"))
                }
            })
        } catch (e: Exception) {
            savePendingIfNeeded(message, destination)
            ack.onFailure(message, e)
        }
    }

    override fun <T> convertAndSend(destination: String, message: MessageExtend<T>) {
        template.send(destination, buildMessage(message))
    }

    private fun <T> buildMessage(message: MessageExtend<T>): Message<MessageExtend<T>> {
        val builder = MessageBuilder.withPayload(message)

        // 自定义 headers
        message.headers.forEach { (k, v) -> builder.setHeader(k, v) }

        // RocketMQ 原生 headers
        message.key.takeIf { it.isNotEmpty() }?.let { builder.setHeader(RocketMQHeaders.KEYS, it) }

        // 延迟消息：delayLevel 1-18 或 5.0+ 支持毫秒
        message.delay?.let { delay ->
            // 延迟级别映射：1=1s, 2=5s, 3=10s, 4=30s, 5=1m, 6=2m, 7=3m, 8=4m, 9=5m, 10=6m, 11=7m, 12=8m, 13=9m, 14=10m, 15=20m, 16=30m, 17=1h, 18=2h
            val delayLevel = when {
                delay < 1000 -> 1
                delay < 5000 -> 2
                delay < 10000 -> 3
                delay < 30000 -> 4
                delay < 60000 -> 5
                delay < 120000 -> 6
                delay < 180000 -> 7
                delay < 240000 -> 8
                delay < 300000 -> 9
                delay < 360000 -> 10
                delay < 420000 -> 11
                delay < 480000 -> 12
                delay < 540000 -> 13
                delay < 600000 -> 14
                delay < 1200000 -> 15
                delay < 1800000 -> 16
                delay < 3600000 -> 17
                else -> 18
            }
            builder.setHeader(RocketMQHeaders.DELAY, delayLevel)
        }

        // 分区顺序消息（自定义 header，消费端用 MessageListenerOrderly）
        message.shardingKey?.let { builder.setHeader("SHARDING_KEY", it) }

        // 优先级（自定义，需要消费端配合）
        message.priority?.let { builder.setHeader("PRIORITY", it) }

        // TTL
        message.ttl?.let { builder.setHeader("TTL_MS", it) }

        // 死信队列
        message.deadLetter?.let { builder.setHeader("DLQ_TOPIC", it) }

        // 重试配置
        builder.setHeader("RETRY", message.retry)
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
