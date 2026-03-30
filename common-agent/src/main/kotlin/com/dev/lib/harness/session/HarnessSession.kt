package com.dev.lib.harness.session

import com.dev.lib.CoroutineScopeHolder
import com.dev.lib.harness.HarnessError
import com.dev.lib.harness.HarnessException
import com.dev.lib.harness.protocol.UiEvent
import com.dev.lib.harness.protocol.UserInput
import kotlinx.coroutines.channels.Channel

interface Session {

    interface Sink {
        fun onEvent(event: UiEvent)
    }

    fun submit(input: UserInput)

    suspend fun emit(event: UiEvent)
}

class HarnessSession(
    val sink: Session.Sink
) : Session {
    val turn = ActiveTurn(this)
    val submissionReceiver = Channel<UserInput>(16)

    val submissionJob = CoroutineScopeHolder.launch {
        for (sub in submissionReceiver) {
            turn.dispatch(sub)
        }
    }

    override fun submit(input: UserInput) {
        if (submissionReceiver.trySend(input).isFailure) {
            throw HarnessException(HarnessError.QUEUE_IS_FULL)
        }
        recordUserPrompt(input)
    }

    fun recordUserPrompt(input: UserInput) {

    }

    val uiEventReceiver = Channel<UiEvent>(Channel.UNLIMITED)

    val uiEventJob = CoroutineScopeHolder.launch {
        for (event in uiEventReceiver) {
            runCatching {
                sink.onEvent(event)
            }.onFailure {

            }
        }
    }

    override suspend fun emit(event: UiEvent) {
        uiEventReceiver.send(event)
    }
}