package com.dev.lib.mq

object MQExtensions {

    fun ack(): AckAction = AckAction.ACK

    fun nack(): AckAction = AckAction.NACK

    fun reject(): AckAction = AckAction.REJECT
}
