package com.dev.lib.mq

import com.dev.lib.mq.AckAction.ACK
import com.dev.lib.mq.AckAction.NACK
import com.dev.lib.mq.AckAction.REJECT

object MQExtensions {

    fun <T> ack(): AckAction = ACK

    fun <T> nack(): AckAction = NACK

    fun <T> retry(): AckAction = REJECT
}
