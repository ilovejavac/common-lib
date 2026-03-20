package com.dev.lib.harness.session

import com.dev.lib.harness.protocol.Command
import com.dev.lib.harness.protocol.OperationContext
import com.dev.lib.harness.protocol.SteerInputError
import com.dev.lib.harness.session.tasks.RegularTask

object OperationHandler {

    suspend fun turn(context: OperationContext) {
        val tc = context.newTurn()
        val session = context.session
        val (items) = context.submission.op as Command.UserTurn

        tc.logUserPrompt(items)

        session.steerInput(items).match<SteerInputError.NoActiveTurn> { (inputs) ->
            session.spawnTask(tc, inputs, RegularTask())
        }
    }

}
