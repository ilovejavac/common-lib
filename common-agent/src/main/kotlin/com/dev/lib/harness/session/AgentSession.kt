package com.dev.lib.harness.session

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.harness.HarnessError
import com.dev.lib.harness.HarnessException
import com.dev.lib.harness.sdk.model.ModelProvider
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    private val sender = CoroutineScopeHolder.launch {
        for (sub in tx) {
            dispatch(sub)
        }
    }

    private fun dispatch(submission: Submission) {
        when (val op = submission.op) {
            is Op.UserInput -> {

            }

            is Op.UserInterrupt -> {

            }
        }
    }

    private val receiver = CoroutineScopeHolder.launch {
        for (msg in rx) {

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
        mutex.withLock {

        }
        tx.close()
        rx.close()

        sender.cancelAndJoin()
        receiver.cancelAndJoin()
    }
}