package com.dev.lib.ai.service.agent

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.ai.model.AceItem
import com.dev.lib.ai.model.ChatMessage
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
    /**
     * 全量记忆
     * */
    override val history: MutableList<ChatMessage> = mutableListOf(),
    /**
     * ACE 行动指南
     */
    override val acePayload: MutableList<AceItem> = mutableListOf()
) : ChatSession {

    override lateinit var response: ChatResponse

    override fun workingMemory(prompt: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

//        messages += ChatMessage.Companion.system(
//            "".format(
//                // ace
//                // keypoint
//            )
//        )

        history.takeLast(10).forEach {
            messages += it
        }

        messages += ChatMessage.Companion.user(prompt)

        return messages
    }

    override fun generate(prompt: String): ChatSSE {
        val sse = ChatSSE()

        val job = CoroutineScopeHolder.launch {
            agent.run(prompt, this@AiChatSession, sse)
        }

        return sse.timeout {
            job.cancel()
        }.error {
            job.cancel()
        }
    }

}