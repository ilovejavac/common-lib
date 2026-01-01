package com.dev.lib.ai.service.llm

import com.dev.lib.ai.model.ChatMessage
import com.dev.lib.ai.model.ChatResponse

interface LLM {
    suspend fun stream(messages: List<ChatMessage>, block: (chunk: String) -> Unit): ChatResponse

    fun call(messages: List<ChatMessage>): String
}