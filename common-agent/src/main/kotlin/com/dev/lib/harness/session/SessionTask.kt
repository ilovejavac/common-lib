package com.dev.lib.harness.session

import com.dev.lib.harness.protocol.SessionTaskContext
import com.dev.lib.harness.protocol.TaskKind
import com.dev.lib.harness.protocol.TurnContext
import com.dev.lib.harness.protocol.UserInput

interface SessionTask {

    val kind: TaskKind

    fun spanName(): String

    suspend fun run(
        context: SessionTaskContext,
        turn: TurnContext,
        input: UserInput
    )

    suspend fun abort(context: SessionTaskContext, turnContext: TurnContext)
}