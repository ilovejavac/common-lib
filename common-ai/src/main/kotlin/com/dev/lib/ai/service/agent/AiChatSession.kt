package com.dev.lib.ai.service.agent

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.ai.model.AceItem
import com.dev.lib.ai.model.ChatItem
import com.dev.lib.ai.model.ChatResponse
import com.dev.lib.ai.model.ChatSSE
import com.dev.lib.ai.service.llm.LLM

/**
 * AI会话
 */
class AiChatSession(
    override val sessionId: String,

    val agent: Agent,

    override val llm: LLM,

    override val history: MutableList<ChatItem> = mutableListOf(),

    override val acePayload: MutableList<AceItem> = mutableListOf()
) : ChatSession {

    override lateinit var response: ChatResponse

    override fun workingMemory(prompt: String): List<ChatItem> {
        val messages = mutableListOf<ChatItem>()

//        messages += ChatItem.system("")
        messages.addAll(history)
        messages += ChatItem.user(prompt)

        return messages
    }

    override fun generate(prompt: String, block: () -> Unit): ChatSSE {
        val sse = ChatSSE()

        val job = CoroutineScopeHolder.launch {
            agent.run(prompt, this@AiChatSession, sse)
            block()
        }

        return sse.timeout {
            job.cancel()
        }.error {
            job.cancel()
        }
    }

}