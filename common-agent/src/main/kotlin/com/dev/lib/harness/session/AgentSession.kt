package com.dev.lib.harness.session

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.entity.id.IDWorker
import com.dev.lib.harness.HarnessError
import com.dev.lib.harness.HarnessException
import com.dev.lib.harness.protocol.*
import com.dev.lib.harness.sdk.model.ModelProvider
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import java.time.Instant

class AgentSession(
    config: AgentSessionBuilder
) {
    val id: String = IDWorker.newId()
    val modelProvider: ModelProvider = config.modelProvider

    companion object {
        fun newBuilder() = AgentSessionBuilder()

        const val SUBMISSION_CHANNEL_CAPACITY = 32
    }

    private val mutex = Mutex()
    private var runtime = SessionRuntime(
        state = SessionState.IDLE
    )
    val state get() = runtime.state

    val tx = Channel<Submission>(SUBMISSION_CHANNEL_CAPACITY)
    val rx = Channel<EventMsg>(Channel.UNLIMITED)

    private val commandReceiver = CoroutineScopeHolder.launch {
        for (sub in tx) {
            dispatch(sub)
        }
    }

    private val eventReceiver = CoroutineScopeHolder.launch {
        for (msg in rx) {

        }
    }

    private suspend fun dispatch(submission: Submission) {
        val context = OperationContext(this, submission)
        when (val op = submission.op) {
            is command.UserTurn -> {
                OperationHandler.turn(context)
            }

            is command.UserInterrupt -> {
                OperationHandler.interrupt(context)
            }

            is command.OverrideTurnContext -> {
                OperationHandler.overrideTurnContext(context)
            }

            is command.ExecApproval -> {
                OperationHandler.approval(context)
            }
        }
    }

    private fun validSessionIsRunning() {
        check(state != SessionState.CLOSED) { "session-$id closed" }
    }

    fun send(sub: Submission) {
        if (tx.trySend(sub).isFailure) {
            throw HarnessException(HarnessError.QUEUE_IS_FULL)
        }
    }

    private suspend fun emit(msg: EventMsg) {
        rx.send(msg)
    }

    suspend fun abort() {
        tx.close()
        rx.close()

        commandReceiver.cancelAndJoin()
        eventReceiver.cancelAndJoin()
    }

    fun newTurn(): TurnContext {
        val turnContext = TurnContext(Instant.now(), IDWorker.newId())


        return turnContext
    }
}

class AgentSessionBuilder {

    lateinit var modelProvider: ModelProvider

    fun modelProvider(modelProvider: ModelProvider): AgentSessionBuilder {
        this.modelProvider = modelProvider
        return this
    }

    fun build(): AgentSession {
        return AgentSession(this)
    }
}