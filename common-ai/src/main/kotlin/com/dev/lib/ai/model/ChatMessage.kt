package com.dev.lib.ai.model

data class ChatMessage(
    val content: String,
    val role: ChatRole,
    val input: Int = 0,
    val output: Int = 0
) {
    companion object {
        fun system(prompt: String): ChatMessage {
            return ChatMessage(prompt, ChatRole.SYSTEM)
        }

        fun user(prompt: String): ChatMessage {
            return ChatMessage(prompt, ChatRole.USER)
        }

        fun assistant(prompt: String): ChatMessage {
            return ChatMessage(prompt, ChatRole.ASSISTANT)
        }
    }
}

enum class ChatRole {
    USER, SYSTEM, ASSISTANT
}