package com.dev.lib.mq.reliability

import com.dev.lib.mq.AckAction
import com.dev.lib.mq.MessageExtend

interface MessageReliabilityHandler {

    fun onSendFailed(message: MessageExtend<*>, e: Throwable)

    fun onConsumed(messageId: String)

    fun onConsumFailed(messageId: String, e: Throwable, action: AckAction)
}
