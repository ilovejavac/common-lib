package com.dev.lib.harness.sdk

import com.dev.lib.harness.protocol.TurnContext
import org.springframework.ai.chat.messages.MessageType

interface Agent {
    suspend fun query(prompts: List<Prompt>, context: TurnContext)
}

sealed interface Prompt {
    data class Message(
        val role: MessageType,
        val content: String
    ) : Prompt
}