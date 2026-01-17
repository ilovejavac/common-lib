package com.dev.lib.mq.reliability

import com.dev.lib.mq.MessageExtend

interface MessageStorage {

    suspend fun save(message: MessageExtend<*>)

    suspend fun markAsConsumed(messageId: String)

    suspend fun getPendingMessages(limit: Int = 100): List<MessageExtend<*>>

    suspend fun delete(messageId: String)
}
