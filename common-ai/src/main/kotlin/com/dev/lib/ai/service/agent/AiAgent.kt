package com.dev.lib.ai.service.agent

import com.dev.lib.ai.model.ChatEnvelope
import com.dev.lib.ai.model.ChatItem
import com.dev.lib.ai.model.ChatSSE
import com.dev.lib.ai.model.event.SessionCompletedEvent
import com.dev.lib.domain.AggregateRoot

class AiAgent : Agent, AggregateRoot() {

    override suspend fun run(
        prompt: String,
        session: ChatSession,
        sse: ChatSSE
    ) {

        session.response = session.llm.stream(session.workingMemory(prompt)) {
            sse.send(ChatEnvelope(it))
        }

        sse.done()

        // 写入 history
        session.history += ChatItem.user(prompt).apply {
            token = session.response.inputTokenCount ?: 0
        }
        session.history += ChatItem.assistant(session.response.content).apply {
            token = session.response.outputTokenCount ?: 0
        }

        marketCompleted(session)
        publishAndClear()
    }

    private fun marketCompleted(session: ChatSession) {
        registerEvent(SessionCompletedEvent(session))
    }
}