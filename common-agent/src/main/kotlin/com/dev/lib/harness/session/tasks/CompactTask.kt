package com.dev.lib.harness.session.tasks

import com.dev.lib.None
import com.dev.lib.Option
import com.dev.lib.harness.protocol.*
import com.dev.lib.harness.session.SessionTask

class CompactTask : SessionTask {
    override val kind: TaskKind = TaskKind.Compact

    override fun spanName(): String = "session_task.compact"

    override suspend fun run(
        context: SessionTaskContext,
        turn: TurnContext,
        inputs: List<UserInput>
    ): Option<String> {
        val session = context.session
        session.emit(
            EventMsg.TurnStarted(
                turn = turn.submissionId,
                modelContextWindow = turn.modelContextWindow()
            )
        )

        return None
    }
}