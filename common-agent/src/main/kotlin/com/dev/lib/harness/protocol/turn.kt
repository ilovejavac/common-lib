package com.dev.lib.harness.protocol

import com.dev.lib.harness.sdk.model.LlmClient
import com.dev.lib.harness.session.Session
import java.time.LocalDateTime
import java.util.*

enum class TurnState {
    IDEL,
    RUNNING,
    PENDING_APPROVE,
    PENDING_INPUT,
}

data class TurnContext(
    val session: Session,

    val submissionId: String,

    // 工作目录
    val cwd: String,

    // 当前时间
    val currentData: LocalDateTime,
    val timezone: TimeZone,

    // 模型配置
    val options: TurnOptions,
    val model: LlmClient,
) {

}

data class TurnOptions(

    val model: String,
    val prompt: String,

    val thinking: Think,
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int
)

data class Think(
    val enable: Boolean
)

interface AgentTask {
    fun run(input: UserInput, context: TurnContext)

    suspend fun abort()
}