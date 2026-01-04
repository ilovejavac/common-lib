package com.dev.lib.ai.model

data class ChatResponse(
    val thinking: String?,
    val content: String,
    val inputTokenCount: Int?,
    val outputTokenCount: Int?,
    val totalTokenCount: Int?,
) {
}