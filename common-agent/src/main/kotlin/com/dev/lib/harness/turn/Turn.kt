package com.dev.lib.harness.turn

import java.time.Instant

data class ActiveTurn (
    val tasks: Map<String, RunningTask>,
    val turnState: TurnState
)

data class RunningTask(
    val turnContext: TurnContext,
    val kind: TaskKind,
    // taskHolder: AbortOnDropHandle(SessionTask)
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