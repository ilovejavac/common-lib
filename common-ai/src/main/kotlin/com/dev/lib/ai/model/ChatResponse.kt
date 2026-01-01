package com.dev.lib.ai.model

data class ChatResponse(
    val thinking: String,
    val content: String,
    val inputTokenCount: Int = 0,
    val outputTokenCount: Int = 0,
    val totalTokenCount: Int = 0,
) {
}