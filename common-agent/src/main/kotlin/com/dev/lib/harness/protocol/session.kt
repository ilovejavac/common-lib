package com.dev.lib.harness.protocol

import com.dev.lib.Outcome
import com.dev.lib.harness.session.AgentSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class SessionRuntime(
    var state: SessionState,
) {

    private val pendingUserInputs: MutableList<UserInput> = mutableListOf()
    private val turnState = TurnState(
        tokenUsage = TokenUsage()
    )
    private val activeTurnMutex = Mutex()

    val tasks: MutableMap<String, RunningTask> = mutableMapOf()

    suspend fun steerInput(
        input: List<UserInput>,
        expectedTurnId: String? = null
    ): Outcome<String, SteerInputError> {
        if (input.isEmpty()) {
            return Outcome.failure(SteerInputError.EmptyInput)
        }

        return activeTurnMutex.withLock {
            val activeTurnId = tasks.keys.firstOrNull()
                ?: return@withLock Outcome.failure(
                    SteerInputError.NoActiveTurn(input)
                )

            if (expectedTurnId != null && expectedTurnId != activeTurnId) {
                return@withLock Outcome.failure(
                    SteerInputError.ExpectedTurnMismatch(
                        expected = expectedTurnId,
                        actual = activeTurnId
                    )
                )
            }

            pendingUserInputs.addAll(input)
            Outcome.success(activeTurnId)
        }
    }

    suspend fun abortAllTasks(reason: TurnAbortReason) {
        activeTurnMutex.withLock {
            state = SessionState.CLOSED
        }

        pendingUserInputs.clear()
    }

    suspend fun registRunningTask(block: () -> RunningTask) {
        activeTurnMutex.withLock {
            state = SessionState.RUNNING
        }

        val runningTask = block()

        tasks[runningTask.turnContext.submissionId] = runningTask
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

data class RunningTask(
    val turnContext: TurnContext,
    val kind: TaskKind,
    val jobHolder: Job,
    val timer: Timer
)

enum class TaskKind {
    Regular,
    Review,
    Compact
}

data class Timer(
    val name: String,
    val startTime: Instant
)

data class TokenUsage(
    val inputTokens: Int = 0,
    val cachedInputTokens: Int = 0,
    val outputTokens: Int = 0,
    val reasoningOutputTokens: Int = 0,
    val totalTokens: Int = 0
)