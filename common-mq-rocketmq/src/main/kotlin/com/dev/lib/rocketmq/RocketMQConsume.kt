package com.dev.lib.rocketmq

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MQ
import com.dev.lib.mq.MessageExtend
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * RocketMQ 消费端扩展函数
 */

private val retryCountCache: Cache<String, AtomicInteger> = Caffeine.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .maximumSize(100_000)
    .build()

/**
 * RocketMQ 带重试的消息处理
 * @return RocketMQ 事务状态
 */
@JvmName("consumeRocket")
fun <T> MQ.consume(
    message: MessageExtend<T>,
    handler: (MessageExtend<T>) -> AckAction
): RocketMQLocalTransactionState {
    val msgId = message.id.toString()
    val currentRetry = retryCountCache.get(msgId) { AtomicInteger(0) }

    val action = try {
        handler(message)
    } catch (e: Exception) {
        if (currentRetry.get() < message.retry) {
            currentRetry.incrementAndGet()
            return RocketMQLocalTransactionState.ROLLBACK
        } else {
            retryCountCache.invalidate(msgId)
            return RocketMQLocalTransactionState.UNKNOWN
        }
    }

    val state = when (action) {
        AckAction.ACK -> {
            retryCountCache.invalidate(msgId)
            RocketMQLocalTransactionState.COMMIT
        }
        AckAction.NACK -> {
            if (currentRetry.get() < message.retry) {
                currentRetry.incrementAndGet()
                RocketMQLocalTransactionState.ROLLBACK
            } else {
                retryCountCache.invalidate(msgId)
                RocketMQLocalTransactionState.UNKNOWN
            }
        }
        AckAction.REJECT -> {
            retryCountCache.invalidate(msgId)
            RocketMQLocalTransactionState.UNKNOWN
        }
    }

    return state
}

/**
 * RocketMQ 不带重试的消息处理
 * @return RocketMQ 事务状态
 */
@JvmName("consumeRocketSimple")
fun <T> MQ.consume(
    message: T,
    handler: (T) -> AckAction
): RocketMQLocalTransactionState {
    val action = try {
        handler(message)
    } catch (e: Exception) {
        AckAction.REJECT
    }
    return toTransactionState(action)
}

private fun toTransactionState(action: AckAction): RocketMQLocalTransactionState {
    return when (action) {
        AckAction.ACK -> RocketMQLocalTransactionState.COMMIT
        AckAction.NACK -> RocketMQLocalTransactionState.ROLLBACK
        AckAction.REJECT -> RocketMQLocalTransactionState.UNKNOWN
    }
}
