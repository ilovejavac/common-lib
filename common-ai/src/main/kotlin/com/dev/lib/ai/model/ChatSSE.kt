package com.dev.lib.ai.model

import com.alibaba.fastjson2.JSON
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Duration

class ChatSSE(
    timeout: Long = Duration.ofMinutes(5).toMillis()
) : SseEmitter(timeout) {

    fun send(envelope: ChatEnvelope<Any>): ChatSSE {
        send("message", envelope)
        return this
    }

    fun error(envelope: ChatEnvelope<Any>): ChatSSE {
        send("error", envelope)
        return this
    }

    fun done(envelope: ChatEnvelope<Any>) {
        send("done", envelope)
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
