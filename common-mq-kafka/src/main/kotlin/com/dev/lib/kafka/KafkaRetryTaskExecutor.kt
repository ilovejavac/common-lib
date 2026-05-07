package com.dev.lib.kafka

import com.dev.lib.log
import com.dev.lib.mq.MQ
import com.dev.lib.mq.MessageExtend
import com.dev.lib.task.annotation.Task
import com.dev.lib.task.domain.ReliableTaskExecutor
import com.dev.lib.task.domain.TaskExecuteContext
import com.dev.lib.task.domain.TaskExecuteResult

@Task("KAFKA_RETRY")
class KafkaRetryTaskExecutor : ReliableTaskExecutor<KafkaRetryPayload> {

    override fun execute(context: TaskExecuteContext<KafkaRetryPayload>): TaskExecuteResult {
        val payload = context.payload
        return try {
            val message = MessageExtend.of(payload.body)
            message.key = payload.key
            payload.headers.forEach { (k, v) -> message.set(k, v) }
            MQ.send(payload.destination, message)
            log.info("Kafka message sent successfully: destination={}, key={}", payload.destination, payload.key)
            TaskExecuteResult.success("KAFKA_SENT")
        } catch (e: Exception) {
            log.error("Failed to send Kafka message: {}", e.message, e)
            TaskExecuteResult.failure(e.message ?: "Unknown error")
        }
    }

}
