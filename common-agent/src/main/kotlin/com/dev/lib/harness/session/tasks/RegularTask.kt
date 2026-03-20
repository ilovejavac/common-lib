package com.dev.lib.harness.session.tasks

import com.dev.lib.harness.protocol.SessionTaskContext
import com.dev.lib.harness.protocol.TaskKind
import com.dev.lib.harness.protocol.TurnContext
import com.dev.lib.harness.protocol.UserInput
import com.dev.lib.harness.session.SessionTask

class RegularTask : SessionTask {
    override val kind: TaskKind
        get() = TaskKind.Regular

    override fun spanName(): String = "session_task.turn"

    override suspend fun run(
        context: SessionTaskContext,
        turn: TurnContext,
        input: UserInput
    ) {

    }

    override suspend fun abort(
        context: SessionTaskContext,
        turnContext: TurnContext
    ) {

    }
}