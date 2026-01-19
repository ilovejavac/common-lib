package com.dev.lib.kafka

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MQ
import com.dev.lib.mq.MessageExtend
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kafka 消费端扩展函数
 */

private val retryCountCache: Cache<String, AtomicInteger> = Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .maximumSize(100_000)
    .build()

/**
 * Kafka 带重试的消息处理
 */
@JvmName("consumeKafka")
fun <T> MQ.consume(
    record: ConsumerRecord<String, Any>,
    acknowledgment: Acknowledgment?,
    handler: (MessageExtend<T>) -> AckAction
) {
    @Suppress("UNCHECKED_CAST")
    val message = record.value() as? MessageExtend<T> ?: return

    val msgId = message.id.toString()
    val currentRetry = retryCountCache.get(msgId) { AtomicInteger(0) }

    val action = try {
        handler(message)
    } catch (e: Exception) {
        if (currentRetry.get() < message.retry) {
            currentRetry.incrementAndGet()
        } else {
            retryCountCache.invalidate(msgId)
        }
        applyAck(AckAction.NACK, acknowledgment)
        return
    }

    when (action) {
        AckAction.ACK -> {
            applyAck(AckAction.ACK, acknowledgment)
            retryCountCache.invalidate(msgId)
        }
        AckAction.NACK -> {
            if (currentRetry.get() < message.retry) {
                currentRetry.incrementAndGet()
                applyAck(AckAction.NACK, acknowledgment)
            } else {
                retryCountCache.invalidate(msgId)
                applyAck(AckAction.REJECT, acknowledgment)
            }
        }
        AckAction.REJECT -> {
            applyAck(AckAction.REJECT, acknowledgment)
            retryCountCache.invalidate(msgId)
        }
    }
}

/**
 * Kafka 不带重试的消息处理
 */
@JvmName("consumeKafkaSimple")
fun <T> MQ.consume(
    record: ConsumerRecord<String, Any>,
    acknowledgment: Acknowledgment?,
    handler: (T) -> AckAction
) {
    @Suppress("UNCHECKED_CAST")
    val message = record.value() as? T ?: return

    val action = try {
        handler(message)
    } catch (e: Exception) {
        AckAction.REJECT
    }
    applyAck(action, acknowledgment)
}

private fun applyAck(action: AckAction, acknowledgment: Acknowledgment?) {
    when (action) {
        AckAction.ACK -> acknowledgment?.acknowledge()
        AckAction.NACK -> acknowledgment?.acknowledge()
        AckAction.REJECT -> {}
    }
}
