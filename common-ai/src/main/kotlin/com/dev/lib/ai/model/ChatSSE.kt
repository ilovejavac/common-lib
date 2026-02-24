package com.dev.lib.ai.model

import com.alibaba.fastjson2.JSON
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AI 对话专用 SSE 封装
 *
 * 特性：
 * - 默认超时为 0（永不过期）
 * - 内置心跳机制，防止连接被中间代理/防火墙断开
 * - 发送数据时自动重置心跳计时
 */
class ChatSSE(
    timeout: Long = 0L,  // 永不过期
    private val heartbeatInterval: Long = 30L  // 心跳间隔 30 秒
) : SseEmitter(timeout) {

    private val completed = AtomicBoolean(false)

    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private var heartbeatTask: ScheduledFuture<*>? = null

    init {
        startHeartbeat()

        // 连接结束时清理资源
        onCompletion {
            stopHeartbeat()
            scheduler.shutdown()
        }
        onTimeout {
            stopHeartbeat()
            scheduler.shutdown()
        }
        onError {
            stopHeartbeat()
            scheduler.shutdown()
        }
    }

    /**
     * 发送消息
     */
    fun send(envelope: ChatEnvelope<Any>): ChatSSE {
        if (completed.get()) {
            return this
        }

        try {
            send("message", envelope)
            resetHeartbeat()  // 发送数据后重置心跳
        } catch (e: Exception) {
            completeWithError(e)
        }
        return this
    }

    /**
     * 发送错误消息
     */
    fun error(envelope: ChatEnvelope<Any>): ChatSSE {
        send("error", envelope)
        return this
    }

    /**
     * 完成 SSE 流
     */
    fun done() {
        if (!completed.compareAndSet(false, true)) {
            return
        }

        stopHeartbeat()
        complete()
    }

    private fun send(event: String, envelope: ChatEnvelope<Any>) {
        try {
            val data = JSON.toJSONString(envelope)
            send(event().name(event).data(data))
        } catch (e: Exception) {
            completeWithError(e)
        }
    }

    override fun completeWithError(ex: Throwable) {
        if (!(completed.compareAndSet(false, true))) {
            return
        }

        stopHeartbeat()
        super.completeWithError(ex)
    }

    fun completion(block: Runnable): ChatSSE {
        onCompletion(block)
        return this
    }

    fun timeout(block: Runnable): ChatSSE {
        onTimeout(block)
        return this
    }

    fun error(block: Runnable): ChatSSE {
        onError {
            block.run()
        }
        return this
    }

    // ==================== 心跳机制 ====================

    /**
     * 启动心跳任务
     */
    private fun startHeartbeat() {
        heartbeatTask = scheduler.scheduleAtFixedRate({
            if (!completed.get()) {
                try {
                    // 使用 SSE comment 格式发送心跳，客户端不会作为事件处理
                    send(SseEmitter.event().comment("heartbeat"))
                } catch (e: Exception) {
                    // 心跳失败，连接可能已断开
                    stopHeartbeat()
                }
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS)
    }

    /**
     * 停止心跳任务
     */
    private fun stopHeartbeat() {
        heartbeatTask?.cancel(false)
        heartbeatTask = null
    }

    /**
     * 重置心跳计时（发送数据后调用）
     */
    private fun resetHeartbeat() {
        stopHeartbeat()
        startHeartbeat()
    }
}
