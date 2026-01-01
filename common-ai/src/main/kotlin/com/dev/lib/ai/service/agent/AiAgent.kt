package com.dev.lib.ai.service.agent

import com.dev.lib.ai.model.ChatEnvelope
import com.dev.lib.ai.model.ChatMessage
import com.dev.lib.ai.model.ChatSSE
import com.dev.lib.ai.model.event.SessionCompletedEvent
import com.dev.lib.domain.AggregateRoot
import com.dev.lib.log

class AiAgent : Agent, AggregateRoot() {

    suspend override fun run(
        prompt: String, session: ChatSession, sse: ChatSSE
    ) {
        log.info("SSE Start")
        val response = session.llm.stream(session.workingMemory(prompt)) {
            log.debug("llm: {}", it)
            sse.send(ChatEnvelope(it))
        }

        sse.done()
        log.info("SSE End")

        // 写入 history
        session.history += ChatMessage.user(prompt)
        session.history += ChatMessage.assistant(response.content)

        marketCompleted(session)
        publishAndClear()
    }

    private fun marketCompleted(session: ChatSession) {
        registerEvent(SessionCompletedEvent(session))
    }
}