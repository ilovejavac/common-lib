package com.dev.lib.harness.session

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.Outcome
import com.dev.lib.entity.id.IDWorker
import com.dev.lib.harness.HarnessError
import com.dev.lib.harness.protocol.*
import com.dev.lib.harness.sdk.model.ModelProvider
import com.dev.lib.harness.sdk.skill.SkillManager
import kotlinx.coroutines.channels.Channel
import java.time.Instant

class AgentSession(
    config: AgentSessionBuilder
) {
    val id: String = IDWorker.newId()
    val modelProvider = config.modelProvider
    val skillManager = config.skillManager

    companion object {
        fun newBuilder() = AgentSessionBuilder()

        const val SUBMISSION_CHANNEL_CAPACITY = 32
        const val  TURN_E2E_DURATION_METRIC = "turn.e2e_duration_ms"
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

    suspend fun submit(sub: Submission) {
        if (xSub.trySend(sub).isFailure) {
            emit(EventMsg.Warning(HarnessError.QUEUE_IS_FULL))
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

    suspend fun <T : SessionTask> spawnTask(tc: TurnContext, inputs: List<UserInput>, task: T) {
        abortAllTasks(TurnAbortReason.Replaced)

        val sessionTaskContext = SessionTaskContext(this)

        runtime.registRunningTask {
            RunningTask(
                turnContext = tc,
                kind = task.kind,
                timer = Timer(TURN_E2E_DURATION_METRIC, Instant.now()),
                jobHolder = CoroutineScopeHolder.launch {
                    val lastAgentMessage = task.run(sessionTaskContext, tc, inputs)
                    // refush()
                    // if !cancelled() {
                    // pending_input
                    emit(EventMsg.TurnCompleted(tc.submissionId, lastAgentMessage))
                    // }
                })
        }

    }

    private suspend fun abortAllTasks(reason: TurnAbortReason) {
        runtime.abortAllTasks(reason)
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
