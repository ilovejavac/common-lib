package com.dev.lib.rocketmq

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.consumer.MQConsumer
import com.dev.lib.mq.consumer.RetryHandler
import org.apache.rocketmq.client.producer.SendResult
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

@Component
class RocketMQConsumer : MQConsumer {

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

object RocketMQHandlerHelper {

    fun <T> handleWithRetry(
        message: MessageExtend<T>,
        handler: (MessageExtend<T>) -> AckAction
    ): RocketMQLocalTransactionState {
        val action = RetryHandler.handleWithRetry(message, handler)
        return applyAck(action)
    }

    fun <T> handle(message: MessageExtend<T>, handler: (MessageExtend<T>) -> AckAction): RocketMQLocalTransactionState {
        val action = RetryHandler.handle(message, handler)
        return applyAck(action)
    }

    fun <T> handle(message: T, handler: (T) -> AckAction): RocketMQLocalTransactionState {
        val action = RetryHandler.handle(message, handler)
        return applyAck(action)
    }

    private fun applyAck(action: AckAction): RocketMQLocalTransactionState {
        return when (action) {
            AckAction.ACK -> RocketMQLocalTransactionState.COMMIT
            AckAction.NACK -> RocketMQLocalTransactionState.ROLLBACK
            AckAction.REJECT -> RocketMQLocalTransactionState.UNKNOWN
        }
    }
}
