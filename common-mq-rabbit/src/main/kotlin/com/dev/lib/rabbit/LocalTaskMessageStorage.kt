package com.dev.lib.rabbit

import com.dev.lib.local.task.message.data.LocalTaskMessagePoToTaskMessageEntityCommandMapper
import com.dev.lib.local.task.message.data.TaskMessageRepository
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageAdapt
import com.dev.lib.local.task.message.domain.model.NotifyType
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand
import com.dev.lib.mq.MessageExtend
import com.dev.lib.mq.reliability.MessageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

@Component
class LocalTaskMessageStorage(
    private val adapt: ILocalTaskMessageAdapt,
    private val repository: TaskMessageRepository,
    private val mapper: LocalTaskMessagePoToTaskMessageEntityCommandMapper
) : MessageStorage {

    override suspend fun save(message: MessageExtend<*>) = withContext(Dispatchers.IO) {
        val cmd = createCommand(message, message.headers["destination"] as? String ?: "default")
        adapt.saveMessage(cmd)
    }

    override suspend fun markAsConsumed(messageId: String) = withContext(Dispatchers.IO) {
        adapt.updateTaskStatusToSuccess(messageId)
    }

    override suspend fun getPendingMessages(limit: Int): List<MessageExtend<*>> = withContext(Dispatchers.IO) {
        val houseNumbers = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val cmdList = adapt.selectByHouseNumber(houseNumbers, "", limit)

        cmdList.mapNotNull { cmd ->
            val payload = cmd.notifyConfig?.mq?.payload
            @Suppress("UNCHECKED_CAST")
            payload?.get("message") as? MessageExtend<*>
        }
    }

    override suspend fun delete(messageId: String) = withContext(Dispatchers.IO) {
        val query = TaskMessageRepository.Query()
        query.taskId = messageId
        repository.physicalDelete().delete(query)
        Unit
    }

    fun saveAsPending(message: MessageExtend<*>, destination: String) {
        val cmd = createCommand(message, destination)
        adapt.saveMessage(cmd)
    }

    private fun createCommand(message: MessageExtend<*>, destination: String) = TaskMessageEntityCommand().apply {
        taskId = message.id.toString()
        taskName = message.headers["task-name"] ?: "mq-task"
        businessId = message.id.toString()
        maxRetry = message.retry
        notifyType = NotifyType.MQ
        notifyConfig = TaskMessageEntityCommand.NotifyConfig().apply {
            mq = TaskMessageEntityCommand.NotifyConfig.Mq().apply {
                this.destination = destination
                payload = mapOf(
                    "message" to message,
                    "body" to message.body,
                    "headers" to message.headers,
                    "key" to message.key
                )
            }
        }
    }
}
