package com.dev.lib.mq.consumer

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object RetryHandler {

    private val retryCountCache: Cache<String, AtomicInteger> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100_000)
        .build()

    fun <T> handleWithRetry(
        message: MessageExtend<T>,
        handler: (MessageExtend<T>) -> AckAction
    ): AckAction {
        val msgId = message.id.toString()
        val currentRetry = retryCountCache.get(msgId) { AtomicInteger(0) }

        return try {
            handler(message).also {
                if (it == AckAction.ACK) {
                    retryCountCache.invalidate(msgId)
                }
            }
        } catch (e: Exception) {
            handleRetryOrReject(message, currentRetry, msgId)
        }
    }

    private fun <T> handleRetryOrReject(
        message: MessageExtend<T>,
        currentRetry: AtomicInteger,
        msgId: String
    ): AckAction {
        return if (currentRetry.get() < message.retry) {
            currentRetry.incrementAndGet()
            AckAction.NACK
        } else {
            retryCountCache.invalidate(msgId)
            AckAction.REJECT
        }
    }

    fun <T> handle(message: T, handler: (T) -> AckAction): AckAction {
        return try {
            handler(message)
        } catch (e: Exception) {
            AckAction.REJECT
        }
    }
}
