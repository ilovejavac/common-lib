package com.dev.lib.mq

import com.dev.lib.mq.reliability.ReliabilityConfig

object MQ {

    private var template: MQTemplate? = null

    fun init(template: MQTemplate) {
        this.template = template
    }

    fun config(config: ReliabilityConfig) {
        template?.setReliabilityConfig(config)
    }

    fun <T> publish(destination: String, message: MessageExtend<T>) {
        template?.send(destination, message) ?: error("MQTemplate not initialized, call MQ.init() first")
    }

    fun <T> publishAsync(
        destination: String,
        message: MessageExtend<T>,
        ack: AckCallback<T>
    ) {
        template?.sendAsync(destination, message, ack) ?: error("MQTemplate not initialized, call MQ.init() first")
    }

    fun <T> publishAsync(destination: String, message: MessageExtend<T>) {
        publishAsync(destination, message, AckCallback.empty())
    }

    fun <T> convertAndSend(destination: String, message: MessageExtend<T>) {
        template?.convertAndSend(destination, message) ?: error("MQTemplate not initialized, call MQ.init() first")
    }
}
