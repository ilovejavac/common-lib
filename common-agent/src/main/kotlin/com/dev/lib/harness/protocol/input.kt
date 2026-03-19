package com.dev.lib.harness.protocol

import com.dev.lib.harness.session.AgentSession
import com.dev.lib.harness.session.Submission

data class UserInput(
    val text: String,
)

sealed interface command {
    data class UserTurn(
        val items: List<UserInput>
    ) : command

    object UserInterrupt : command

    data class OverrideTurnContext(
        val model: String?

    ) : command

    data class ExecApproval(
        val submission: String,
        val decision: ReviewDecision
    ) : command
}

data class OperationContext(
    val session: AgentSession,
    val submission: Submission
)

enum class ReviewDecision {
    Approved,
    Denied,
    Abort
}
