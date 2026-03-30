package com.dev.lib.harness.protocol

enum class ReviewDecision {
    Approved,
    Denied,
    Abort
}

data class UserResponse(
    val answers: Map<String, Answer>
) {
    data class Answer(
        val answers: List<String>
    )
}

sealed interface UserMessage {
    data class Text(
        val text: String
    ) : UserMessage
}

sealed interface UserInput {

    data class Prompt(
        val turnId: String,
        val items: List<UserMessage>,
        val options: TurnOptions
    ) : UserInput

    data class Approval(
        val turnId: String,
        val decision: ReviewDecision
    ) : UserInput

    data class Interrupt(
        val submissionId: String
    ) : UserInput

    data class Answer(
        val turnId: String,
        val response: UserResponse
    ) : UserInput
}

sealed interface UiEvent {
    data class TurnStarted(
        val submissionId: String
    ) : UiEvent

    data class TurnComplete(
        val submissionId: String
    ) : UiEvent

    data class TurnAborted(
        val submissionId: String
    ) : UiEvent

    data class AgentMessageDelta(
        val submissionId: String,
        val delta: String
    ) : UiEvent

    data class ToolCall(
        val callId: String,
        val tool: String,
        val args: String
    ) : UiEvent

    data class ToolResult(
        val callId: String,
        val tool: String,
        val result: String
    ) : UiEvent

    data class AgentThinkingDelta(
        val submissionId: String,
        val delta: String
    ) : UiEvent

}