package com.dev.lib.harness.session

import com.dev.lib.Option
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
        inputs: List<UserInput>
    ): Option<String>

    suspend fun onAbort(context: SessionTaskContext, turnContext: TurnContext) {

    }
}