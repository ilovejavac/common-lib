package com.dev.lib.ai.service.llm

import com.dev.lib.ai.model.ChatItem
import com.dev.lib.ai.model.ChatResponse

interface LLM {
    suspend fun stream(messages: List<ChatItem>, block: (chunk: String) -> Unit): ChatResponse

    fun call(messages: List<ChatItem>): String
}