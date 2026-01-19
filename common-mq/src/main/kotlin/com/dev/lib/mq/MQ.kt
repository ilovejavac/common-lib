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

    // ========== 发送消息 ==========

    /**
     * 发送消息
     */
    fun <T> send(destination: String, message: MessageExtend<T>) {
        template?.send(destination, message) ?: error("MQTemplate not initialized, call MQ.init() first")
    }

    /**
     * 发送消息（简写，支持 lambda 配置）
     */
    fun <T> send(destination: String, body: T, block: MessageExtend<T>.() -> Unit = {}) {
        send(destination, MessageExtend(body).apply(block))
    }

    /**
     * 异步发送消息
     */
    fun <T> sendAsync(
        destination: String,
        message: MessageExtend<T>,
        ack: AckCallback<T> = AckCallback.empty()
    ) {
        template?.sendAsync(destination, message, ack) ?: error("MQTemplate not initialized, call MQ.init() first")
    }

    /**
     * 异步发送消息（简写）
     */
    fun <T> sendAsync(
        destination: String,
        body: T,
        ack: AckCallback<T> = AckCallback.empty(),
        block: MessageExtend<T>.() -> Unit = {}
    ) {
        sendAsync(destination, MessageExtend(body).apply(block), ack)
    }

    // ========== ACK 快捷方法 ==========

    fun ack(): AckAction = AckAction.ACK
    fun nack(): AckAction = AckAction.NACK
    fun reject(): AckAction = AckAction.REJECT
}
