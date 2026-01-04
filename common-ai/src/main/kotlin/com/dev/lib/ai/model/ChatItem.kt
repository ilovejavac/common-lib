package com.dev.lib.ai.model

data class ChatItem(
    val content: String,
    val role: ChatRole,
    var token: Int = 0,
) {
    companion object {
        fun system(prompt: String): ChatItem {
            return ChatItem(prompt, ChatRole.SYSTEM)
        }

        fun user(prompt: String): ChatItem {
            return ChatItem(prompt, ChatRole.USER)
        }

        fun assistant(prompt: String): ChatItem {
            return ChatItem(prompt, ChatRole.ASSISTANT)
        }
    }
}

enum class ChatRole {
    USER, SYSTEM, ASSISTANT
}