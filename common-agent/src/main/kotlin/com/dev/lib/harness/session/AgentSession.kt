package com.dev.lib.harness.session

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.Outcome
import com.dev.lib.entity.id.IDWorker
import com.dev.lib.harness.HarnessError
import com.dev.lib.harness.HarnessException
import com.dev.lib.harness.protocol.*
import com.dev.lib.harness.sdk.model.ModelProvider
import com.dev.lib.harness.sdk.skill.SkillManager
import kotlinx.coroutines.channels.Channel

class AgentSession(
    config: AgentSessionBuilder
) {
    val id: String = IDWorker.newId()
    val modelProvider = config.modelProvider
    val skillManager = config.skillManager

    companion object {
        fun newBuilder() = AgentSessionBuilder()

        const val SUBMISSION_CHANNEL_CAPACITY = 32
    }

    private var runtime = SessionRuntime(
        state = SessionState.IDLE
    )
    val state get() = runtime.state

    val xSub = Channel<Submission>(SUBMISSION_CHANNEL_CAPACITY)
    val xEvent = Channel<EventMsg>(Channel.UNLIMITED)

    private val commandReceiver = CoroutineScopeHolder.launch {
        for (sub in xSub) {
            dispatch(sub)
        }
    }

    private val eventReceiver = CoroutineScopeHolder.launch {
        for (msg in xEvent) {

        }
    }

    private suspend fun dispatch(submission: Submission) {
        val context = OperationContext(this, submission)
        when (val op = submission.op) {
            is Command.UserTurn -> {
                OperationHandler.turn(context)
            }
        }
    }

    fun send(sub: Submission) {
        if (xSub.trySend(sub).isFailure) {
            throw HarnessException(HarnessError.QUEUE_IS_FULL)
        }
    }

    suspend fun emit(msg: EventMsg) {
        xEvent.send(msg)
    }

    suspend fun steerInput(
        input: List<UserInput>, turnId: String? = null
    ): Outcome<String, SteerInputError> {
        return runtime.steerInput(input, turnId)
    }

    fun <T : SessionTask> spawnTask(tc: TurnContext, inputs: List<UserInput>, task: T) {
//        abortTasks(TurnAbortReason.Replaced)

    }

    fun abortTasks(reason: TurnAbortReason) {

    }
}

class AgentSessionBuilder {

    lateinit var modelProvider: ModelProvider
    lateinit var skillManager: SkillManager

    fun modelProvider(modelProvider: ModelProvider): AgentSessionBuilder {
        this.modelProvider = modelProvider
        return this
    }

    fun skillManager(skillManager: SkillManager): AgentSessionBuilder {
        this.skillManager = skillManager
        return this
    }

    fun build(): AgentSession {
        return AgentSession(this)
    }
}
