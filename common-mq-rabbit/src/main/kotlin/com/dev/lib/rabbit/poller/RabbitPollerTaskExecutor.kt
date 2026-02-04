package com.dev.lib.rabbit.poller

import com.dev.lib.local.task.message.poller.core.PollerContext
import com.dev.lib.local.task.message.poller.core.PollerResult
import com.dev.lib.local.task.message.poller.core.PollerTaskExecutor
import com.dev.lib.local.task.message.poller.core.TaskType
import com.dev.lib.log
import com.dev.lib.mq.MQ
import com.dev.lib.mq.MessageExtend
import org.slf4j.LoggerFactory

/**
 * RabbitMQ Poller 任务执行器
 * 实现 PollerTaskExecutor 接口，处理 RabbitMQ 消息重发
 */
@TaskType("RABBIT_RETRY")
class RabbitPollerTaskExecutor : PollerTaskExecutor {

    override fun execute(context: PollerContext): PollerResult {
        val payload = context.payload

        return try {
            // 从 payload 中提取消息参数
            val destination = payload["destination"] as? String
                ?: return PollerResult.failure("destination is required")

            val body = payload["body"]
                ?: return PollerResult.failure("body is required")

            val routingKey = payload["routingKey"] as? String ?: ""

            @Suppress("UNCHECKED_CAST")
            val headers = payload["headers"] as? Map<String, String> ?: emptyMap()

            val persistent = payload["persistent"] as? Boolean ?: true

            // 构建消息
            val message = MessageExtend.of(body)
            headers.forEach { (k, v) -> message.set(k, v) }
            message.persistent = persistent

            // 设置 routing key
            if (routingKey.isNotEmpty()) {
                message.set("routingKey", routingKey)
            }

            // 发送消息
            MQ.send(destination, message)

            log.info("RabbitMQ message sent successfully: destination={}, routingKey={}", destination, routingKey)
            PollerResult.success("RABBIT_SENT")
        } catch (e: Exception) {
            log.error("Failed to send RabbitMQ message: {}", e.message, e)
            PollerResult.retry(e.message ?: "Unknown error")
        }
    }

}
