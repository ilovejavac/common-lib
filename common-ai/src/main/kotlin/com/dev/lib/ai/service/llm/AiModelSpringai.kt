package com.dev.lib.ai.service.llm

import com.dev.lib.ai.model.ChatMessage
import com.dev.lib.ai.model.ChatResponse
import com.dev.lib.ai.model.ModelEndpoint
import java.math.BigDecimal

data class AiModelSpringai(
    val model: String,
    val endpoint: ModelEndpoint,
    val baseUrl: String,  // 用户自定义 baseUrl，或用默认值
    val apiKey: String,
    val temperature: BigDecimal?,
    val topP: BigDecimal?,
    val maxTokens: Int?
) : LLM {
    override suspend fun stream(
        messages: List<ChatMessage>,
        block: (chunk: String) -> Unit
    ): ChatResponse {
        TODO("Not yet implemented")
    }

    override fun call(messages: List<ChatMessage>): String {
        TODO("Not yet implemented")
    }

    private fun List<ChatMessage>.toSpringai() = map {

    }
}