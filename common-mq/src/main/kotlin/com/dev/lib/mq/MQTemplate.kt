package com.dev.lib.mq

import com.dev.lib.mq.reliability.ReliabilityConfig

interface MQTemplate {

    fun <T> send(destination: String, message: MessageExtend<T>)

    fun <T> sendAsync(
        destination: String,
        message: MessageExtend<T>,
        ack: AckCallback<T>
    )

    fun <T> sendAsync(
        destination: String,
        message: MessageExtend<T>
    ) {
        sendAsync(destination, message, AckCallback.empty())
    }

    fun <T> convertAndSend(destination: String, message: MessageExtend<T>)

    fun setReliabilityConfig(config: ReliabilityConfig)
}
