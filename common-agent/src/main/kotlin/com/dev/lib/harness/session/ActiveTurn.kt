package com.dev.lib.harness.session

import com.dev.lib.harness.protocol.AgentTask
import com.dev.lib.harness.protocol.TurnContext
import com.dev.lib.harness.protocol.TurnState
import com.dev.lib.harness.protocol.UserInput
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ActiveTurn(
    val session: Session
) {
    private var state = TurnState.IDEL
    private val tasks: MutableMap<String, AgentTask> = mutableMapOf()
    private val pendingUserInput: MutableList<UserInput> = mutableListOf()

    private val _stateMutex = Mutex()
    private suspend fun <T> TurnState.lock(block: () -> T): T {
        _stateMutex.withLock {
            return block()
        }
    }

    suspend fun dispatch(input: UserInput) {

        when (input) {
            is UserInput.Prompt -> {
                this.handleInput(input)
            }

            is UserInput.Answer -> {
                this.handleAnswer(input)
            }

            is UserInput.Approval -> {
                this.handleApproval(input)
            }

            is UserInput.Interrupt -> {
                this.handleInterrupt(input)
            }
        }
    }

    private suspend fun steerInput(userInput: UserInput): Boolean {
        val noActiveTurn = state.lock { return@lock state == TurnState.IDEL }
        if (!noActiveTurn) {
            pendingUserInput.add(userInput)
        }
        return !noActiveTurn
    }

    suspend fun spawnTask(context: TurnContext, input: UserInput, task: AgentTask) {
        state.lock { state = TurnState.RUNNING }
        task.run(input, context, session)

        tasks[context.submissionId] = task
    }

    suspend fun handleInput(userInput: UserInput.Prompt) {
        if (!steerInput(userInput)) {
            val context = buildTurnContext(userInput)
            spawnTask(
                context,
                userInput,
                RegularTask()
            )
        }
    }

    private fun buildTurnContext(input: UserInput): TurnContext {

    }

    suspend fun handleAnswer(userInput: UserInput.Answer) {

    }

    suspend fun handleApproval(userInput: UserInput.Approval) {

    }

    suspend fun handleInterrupt(userInput: UserInput.Interrupt) {
        for (task in tasks.values) {
            task.abort()
        }
        pendingUserInput.clear()
    }

}