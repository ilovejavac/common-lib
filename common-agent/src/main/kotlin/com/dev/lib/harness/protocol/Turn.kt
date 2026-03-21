package com.dev.lib.harness.protocol

import com.dev.lib.None
import com.dev.lib.Option
import java.time.Instant


data class TurnContext(
    val cwd: String,
    val currentDate: Instant,
    val submissionId: String,
    val modelInfo: ModelInfo
) {
    fun modelContextWindow(): Int {
        return 258000
    }

    fun logUserPrompt(items: List<UserInput>) {
        // todo trace
    }
}

data class ModelInfo(
    val displayName: String,
    val contextWindow: Int,

    val description: Option<String> = None
) {
    val autoCompactTokenLimit: Int = contextWindow * 9 / 10
}

enum class TurnAbortReason {
    Interrupted,
    Replaced,
    ReviewEnded
}

data class TurnState(
    val pendingApproval: MutableMap<String, ReviewDecision> = mutableMapOf(),
    val pendingUserInput: MutableMap<String, UserInput> = mutableMapOf(),
    val pendingTool: MutableMap<String, String> = mutableMapOf(),

    val tokenUsage: TokenUsage
)