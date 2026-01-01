package com.dev.lib.ai.model

import com.alibaba.fastjson2.JSON
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class ChatSSE(
    timeout: Long = Duration.ofMinutes(5).toMillis()
) : SseEmitter(timeout) {

    private val completed = AtomicBoolean(false)

    fun send(envelope: ChatEnvelope<Any>): ChatSSE {
        if (completed.get()) {
            return this
        }

        try {
            send("message", envelope)
        } catch (e: Exception) {
            completeWithError(e)
        }
        return this
    }

    fun error(envelope: ChatEnvelope<Any>): ChatSSE {
        send("error", envelope)
        return this
    }

    fun done() {
        if (!completed.compareAndSet(false, true)) {
            return
        }

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

}
