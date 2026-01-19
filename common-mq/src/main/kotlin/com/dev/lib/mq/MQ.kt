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

    fun <T> send(destination: String, message: MessageExtend<T>) {
        template?.send(destination, message) ?: error("MQTemplate not initialized, call MQ.init() first")
    }

    fun <T> send(destination: String, body: T, block: MessageExtend<T>.() -> Unit = {}) {
        send(destination, MessageExtend(body).apply(block))
    }

    fun <T> sendAsync(
        destination: String,
        message: MessageExtend<T>,
        ack: AckCallback<T> = AckCallback.empty()
    ) {
        template?.sendAsync(destination, message, ack) ?: error("MQTemplate not initialized, call MQ.init() first")
    }

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

    // ========== 消费扩展点（各模块通过扩展函数实现）==========

    /**
     * 消费消息扩展点
     * 各 MQ 模块通过扩展函数实现具体逻辑
     *
     * 例如 RabbitMQ:
     * fun <T> MQ.consume(message: MessageExtend<T>, channel: Channel, deliveryTag: Long, handler: (MessageExtend<T>) -> AckAction)
     *
     * 例如 RocketMQ:
     * fun <T> MQ.consume(message: MessageExtend<T>, handler: (MessageExtend<T>) -> AckAction)
     */
}
