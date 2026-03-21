package com.dev.lib.harness.protocol

import com.dev.lib.harness.session.AgentSession
import com.dev.lib.harness.session.Submission
import java.time.Instant

data class UserInput(
    val text: String,
    val image: String,
    val skill: String
)

sealed interface Command {
    data class UserTurn(
        val items: List<UserInput>
    ) : Command
}

data class OperationContext(
    val session: AgentSession,
    val submission: Submission
) {
    fun newTurn(): TurnContext {

        return TurnContext(
            cwd = "",
            currentDate = Instant.now(),
            submissionId = submission.id,
            modelInfo = ModelInfo(
                displayName = "",
                contextWindow = 1
            )
        )
    }
}

enum class ReviewDecision {
    Approved,
    Denied,
    Abort
}
