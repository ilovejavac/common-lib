package com.dev.lib.harness.protocol

import java.time.Instant

data class Turn(
    val id: String,
    val status: TurnStatus
)

data class TurnContext(
    val currentDate: Instant,

    val submission: String,
)

enum class TurnStatus {
    Completed,
    Interrupted,
    Failed,
    InProgress
}

enum class TurnState {
    PendingApproval,
    PendingInput,
    ToolCall
}

data class TurnPlanStep(
    val step: String,
    val status: TurnPlanStepStatus
)

enum class TurnPlanStepStatus {
    Pending,
    InProgress,
    Completed
}