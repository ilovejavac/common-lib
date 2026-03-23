package com.dev.lib.harness.protocol

import com.dev.lib.Option
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.metadata.Usage

data class RequestInput(
    val model: String,
    val message: String,
    val tools: List<Any>
)

sealed interface ResponseEvent {
    object Created : ResponseEvent

    data class OutputItemDone(
        val item: ResponseItem
    ) : ResponseEvent

    data class OutputItemAdded(
        val item: ResponseItem
    ) : ResponseEvent

    data class Completed(
        val responseId: String,
        val tokenUsage: Option<Usage>
    ) : ResponseEvent

    data class OutputTextDelta(
        val content: String
    ) : ResponseEvent

}

sealed interface ResponseItem {
    data class Message(
        val id: Option<String>,
        val role: MessageType,
        val content: String,
        val endTurn: Boolean,
        val phase: Option<MessagePhase>
    ) : ResponseItem


}

enum class MessagePhase {
    Commentary,
    FinalAnswer
}
