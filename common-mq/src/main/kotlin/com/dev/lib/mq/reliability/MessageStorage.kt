package com.dev.lib.mq.reliability

import com.dev.lib.mq.MessageExtend

interface MessageStorage {

    fun saveBlocking(message: MessageExtend<*>)

    fun markAsConsumedBlocking(messageId: String)

    fun getPendingMessagesBlocking(limit: Int = 100): List<MessageExtend<*>>

    fun deleteBlocking(messageId: String)
}
