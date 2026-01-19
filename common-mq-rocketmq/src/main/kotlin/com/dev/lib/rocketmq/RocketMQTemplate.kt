package com.dev.lib.rocketmq

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.local.task.message.storage.LocalTaskMessageStorage
import com.dev.lib.mq.AckCallback
import com.dev.lib.mq.MQTemplate
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.reliability.ReliabilityConfig
import org.apache.rocketmq.client.producer.SendCallback
import org.apache.rocketmq.client.producer.SendResult
import org.apache.rocketmq.spring.core.RocketMQTemplate
import org.apache.rocketmq.spring.support.RocketMQHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder

class RocketMQTemplate(
    private val template: RocketMQTemplate,
    private val messageStorage: LocalTaskMessageStorage?
) : MQTemplate {

    private var reliabilityConfig: ReliabilityConfig = ReliabilityConfig.DEFAULT

    override fun setReliabilityConfig(config: ReliabilityConfig) {
        this.reliabilityConfig = config
    }

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

        message.headers.forEach { (k, v) -> builder.setHeader(k, v) }

        message.key.takeIf { it.isNotEmpty() }?.let { builder.setHeader(RocketMQHeaders.KEYS, it) }

        message.delay?.let { delay ->
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
                delay < 60000 -> 14
                delay < 1200000 -> 15
                delay < 1800000 -> 16
                delay < 3600000 -> 17
                else -> 18
            }
            builder.setHeader(RocketMQHeaders.DELAY, delayLevel)
        }

        message.shardingKey?.let { builder.setHeader("SHARDING_KEY", it) }
        message.priority?.let { builder.setHeader("PRIORITY", it) }
        message.ttl?.let { builder.setHeader("TTL_MS", it) }
        message.deadLetter?.let { builder.setHeader("DLQ_TOPIC", it) }
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
