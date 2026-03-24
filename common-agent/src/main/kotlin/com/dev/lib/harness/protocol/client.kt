package com.dev.lib.harness.protocol

import com.dev.lib.Option
import org.springframework.ai.chat.messages.*


sealed interface RequestItem {
    data class Message(
        val id: String,
        val role: MessageType,
        val content: String,
        val endTurn: Boolean,
        val phase: Option<MessagePhase>
    ) : RequestItem


}

enum class MessagePhase {
    Commentary,
    FinalAnswer
}

data class Prompt(
    val input: List<RequestItem>,
    val parallelToolCalls: Boolean,
    val tools: List<Any>
) {

    fun toFormatedInput(): List<Message> {
        return input.mapNotNull {
            when (it) {
                is RequestItem.Message -> {
                    when (it.role) {
                        MessageType.USER -> UserMessage(it.content)
                        MessageType.ASSISTANT -> AssistantMessage(it.content)
                        MessageType.SYSTEM -> SystemMessage(it.content)
                        MessageType.TOOL -> {
                            null
                        }
                    }
                }
            }
            // end of map
        }
        // end of return
    }

}

data class QueryOptions(
    val turnContext: TurnContext
)