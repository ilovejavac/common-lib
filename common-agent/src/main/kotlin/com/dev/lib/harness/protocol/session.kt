package com.dev.lib.harness.protocol

import com.dev.lib.Outcome
import com.dev.lib.harness.session.AgentSession
import com.dev.lib.harness.session.SessionTask
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

class SessionRuntime(
    val state: SessionState,
) {

    private val activeTurnMutex = Mutex()
    private var _activeTurn: ActiveTurn? = null

    suspend fun steerInput(
        input: List<UserInput>, turnId: String? = null
    ): Outcome<String, SteerInputError> {
        if (input.isEmpty()) {
            return Outcome.failure(SteerInputError.EmptyInput)
        }

        return activeTurnMutex.withLock {
            val activeTurn = _activeTurn
                ?: return Outcome.failure(SteerInputError.NoActiveTurn(input))

            val turn = when {
                turnId != null && turnId in activeTurn.tasks -> turnId
                turnId == null -> activeTurn.tasks.entries.firstOrNull()?.key
                else -> null
            } ?: return Outcome.failure(SteerInputError.NoActiveTurn(input))

            activeTurn.pushPendingInput(input)

            Outcome.success(turn)
        }
    }
}

sealed interface SteerInputError {
    data class NoActiveTurn(
        val items: List<UserInput>
    ) : SteerInputError

    data class ExpectedTurnMismatch(
        val expected: String,
        val actual: String
    ) : SteerInputError

    object EmptyInput : SteerInputError
}

data class SessionTaskContext(
    val session: AgentSession
)

enum class SessionState {
    IDLE, RUNNING, CLOSED
}

class ActiveTurn(
    val tasks: Map<String, RunningTask>,
    val turnState: TurnState
) {
    private val _pendingInput = ConcurrentLinkedQueue<UserInput>()
    val pendingInput get() = _pendingInput  // 外部只读

    fun pushPendingInput(input: List<UserInput>) {
        _pendingInput.addAll(input)
    }

}

data class RunningTask(
    val turnContext: TurnContext,
    val kind: TaskKind,
    val holder: AbortOnDropHandle,
    val timer: Timer
)

data class AbortOnDropHandle(
    val sessionTask: SessionTask
) {

    fun abort() {

    }

}

enum class TaskKind {
    Regular,
    Review,
    Compact
}

data class Timer(
    val name: String,
    val startTime: Instant
)