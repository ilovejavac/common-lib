package com.dev.lib.harness.protocol

import java.time.Instant

data class Turn(
    val id: String,
    val status: TurnStatus
)

data class TurnContext(
    val cwd: String,
    val currentDate: Instant,

    val submissionId: String,
) {
    fun modelContextWindow(): Int {
        return 258000
    }

    fun logUserPrompt(items: List<UserInput>) {

    }
}

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

enum class TurnAbortReason {
    Interrupted,
    Replaced,
    ReviewEnded
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