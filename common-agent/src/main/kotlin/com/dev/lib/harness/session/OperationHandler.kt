package com.dev.lib.harness.session

import com.dev.lib.harness.protocol.command
import com.dev.lib.harness.protocol.OperationContext
import com.dev.lib.harness.protocol.ReviewDecision

object OperationHandler {
    suspend fun interrupt(context: OperationContext) {

    }

    suspend fun overrideTurnContext(context: OperationContext) {

    }

    suspend fun turn(context: OperationContext) {
        val (
            items
        ) = context.submission.op as command.UserTurn

        val turnContext = context.session.newTurn()

        // user prompt(items)

    }

    suspend fun approval(context: OperationContext) {
        val (
            sub,
            decision
        ) = context.submission.op as command.ExecApproval

        if (decision == ReviewDecision.Abort) {

        }
    }
}