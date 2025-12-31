package com.dev.lib.ai.service.agent.session

import com.dev.lib.ai.model.ChatSSE
import com.dev.lib.ai.service.agent.Agent
import com.dev.lib.ai.service.agent.ChatSession
import com.dev.lib.ai.service.agent.cache.ResultCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * AI会话
 */
class AiChatSession(
    val sessionId: String,
    val scope: CoroutineScope,
    val agent: Agent,

    ) : ChatSession {
    override var answer: String = ""
    val cache = ResultCache()

    // ace

    // tools

    fun chat(task: String): ChatSSE {
        val sse = ChatSSE()

        val job = scope.launch {
            agent.run(task, this@AiChatSession, sse)
        }

        return sse.completion {
            job.cancel()
        }.timeout {
            job.cancel()
        }.error {
            job.cancel()
        }
    }


}