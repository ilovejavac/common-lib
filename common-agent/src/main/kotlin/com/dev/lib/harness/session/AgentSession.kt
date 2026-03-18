package com.dev.lib.harness.session

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.harness.HarnessError
import com.dev.lib.harness.HarnessException
import com.dev.lib.harness.sdk.model.ModelProvider
import com.dev.lib.harness.turn.ActiveTurn
import com.dev.lib.harness.turn.TurnContext
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

data class AgentSession(
    val id: String,
    val modelProvider: ModelProvider
) {

    private val mutex = Mutex()
    private var runtime = SessionRuntime(
        state = SessionState.IDLE
    )
    val state get() = runtime.state

    val tx = Channel<Submission>(16)
    val rx = Channel<EventMsg>(Channel.BUFFERED)

    lateinit var activeTurn: ActiveTurn

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
            is Op.UserTurn -> {
                OperationHandlers.userInput(context)
            }

            is Op.UserInterrupt -> {
                OperationHandlers.interrupt(context)
            }

            is Op.OverrideTurnContext -> {
                OperationHandlers.overrideTurnContext(context)
            }

            is Op.ExecApproval -> {
                OperationHandlers.execApproval(context)
            }
        }
    }

    private fun validSessionIsRunning() {
        check(state != SessionState.CLOSED) { "session-$id closed" }
    }

    suspend fun send(sub: Submission) {
        validSessionIsRunning()
        mutex.withLock {

        }
        emit(EventMsg.UserMessageEvent(message = ""))
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
        val turnContext = TurnContext(Instant.now(), "")


        return turnContext
    }
}