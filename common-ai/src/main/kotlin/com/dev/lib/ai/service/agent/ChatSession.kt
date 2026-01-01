package com.dev.lib.ai.service.agent

import com.dev.lib.ai.model.AceItem
import com.dev.lib.ai.model.ChatMessage
import com.dev.lib.ai.model.ChatSSE
import com.dev.lib.ai.service.llm.LLM

/**
 * 当前会话
 */
interface ChatSession {
    val sessionId: String

    val llm: LLM

    /**
     * 全量记忆
     * */
    val history: MutableList<ChatMessage>

    // ace
    val acePayload: MutableList<AceItem>

    fun workingMemory(prompt: String): List<ChatMessage>

    fun generate(prompt: String): ChatSSE
}