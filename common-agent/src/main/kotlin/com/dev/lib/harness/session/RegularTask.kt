package com.dev.lib.harness.session

import com.dev.lib.harness.protocol.AgentTask
import com.dev.lib.harness.protocol.TurnContext
import com.dev.lib.harness.protocol.UserInput
import kotlinx.coroutines.Job

class RegularTask(

) : AgentTask {

    private var job: Job? = null

    override fun run(input: UserInput, context: TurnContext) {
        val (session) = context
    }

    override suspend fun abort() {
        job?.cancel()
    }
}
