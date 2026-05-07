package com.dev.lib.rabbit.poller

import com.dev.lib.log
import com.dev.lib.mq.MQ
import com.dev.lib.mq.MessageExtend
import com.dev.lib.task.annotation.Task
import com.dev.lib.task.domain.ReliableTaskExecutor
import com.dev.lib.task.domain.TaskExecuteContext
import com.dev.lib.task.domain.TaskExecuteResult

/**
 * RabbitMQ 可靠重试任务执行器
 * 处理 RabbitMQ 消息重发
 */
@Task("RABBIT_RETRY")
class RabbitPollerTaskExecutor : ReliableTaskExecutor<RabbitRetryPayload> {

    override fun execute(context: TaskExecuteContext<RabbitRetryPayload>): TaskExecuteResult {
        val payload = context.payload

        return try {
            val message = MessageExtend.of(payload.body)
            message.key = payload.routingKey
            payload.headers.forEach { (k, v) -> message.set(k, v) }
            message.persistent = payload.persistent
            MQ.send(payload.destination, message)
            log.info("RabbitMQ message sent successfully: destination={}, routingKey={}", payload.destination, payload.routingKey)
            TaskExecuteResult.success("RABBIT_SENT")
        } catch (e: Exception) {
            log.error("Failed to send RabbitMQ message: {}", e.message, e)
            TaskExecuteResult.failure(e.message ?: "Unknown error")
        }
    }

}
