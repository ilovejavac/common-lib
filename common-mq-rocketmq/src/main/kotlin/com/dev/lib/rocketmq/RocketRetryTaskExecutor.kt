package com.dev.lib.rocketmq

import com.dev.lib.log
import com.dev.lib.mq.MQ
import com.dev.lib.mq.MessageExtend
import com.dev.lib.task.annotation.Task
import com.dev.lib.task.domain.ReliableTaskExecutor
import com.dev.lib.task.domain.TaskExecuteContext
import com.dev.lib.task.domain.TaskExecuteResult

@Task("ROCKET_RETRY")
class RocketRetryTaskExecutor : ReliableTaskExecutor<RocketRetryPayload> {

    override fun execute(context: TaskExecuteContext<RocketRetryPayload>): TaskExecuteResult {
        val payload = context.payload
        return try {
            val message = MessageExtend.of(payload.body)
            message.key = payload.key
            payload.headers.forEach { (k, v) -> message.set(k, v) }
            MQ.send(payload.destination, message)
            log.info("RocketMQ message sent successfully: destination={}, key={}", payload.destination, payload.key)
            TaskExecuteResult.success("ROCKET_SENT")
        } catch (e: Exception) {
            log.error("Failed to send RocketMQ message: {}", e.message, e)
            TaskExecuteResult.failure(e.message ?: "Unknown error")
        }
    }

}
