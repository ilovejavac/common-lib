package com.dev.lib.harness.session

import com.dev.lib.harness.model.ReviewDecision
import com.dev.lib.harness.model.UserInput

data class Submission(
    val id: String,
    val op: Op
)



sealed interface Op {
    data class UserTurn(
        val items: List<UserInput>
    ) : Op

    object UserInterrupt : Op

    data class OverrideTurnContext(
        val model: String?

    ) : Op

    data class ExecApproval(
        val submission: String,
        val decision: ReviewDecision
    ) : Op
}

data class OperationContext(
    val session: AgentSession,
    val submission: Submission
)

object OperationHandlers {
    suspend fun interrupt(context: OperationContext) {

    }

    suspend fun overrideTurnContext(context: OperationContext) {

    }

    suspend fun userInput(context: OperationContext) {
        val (
            items
        ) = context.submission.op as Op.UserTurn

        val turnContext = runCatching {
            context.session.newTurn()
        }.getOrNull() ?: return

        // user prompt(items)

    }

    suspend fun execApproval(context: OperationContext) {
        val (
            sub,
            decision
        ) = context.submission.op as Op.ExecApproval

        if (decision == ReviewDecision.Abort) {

        }
    }
}