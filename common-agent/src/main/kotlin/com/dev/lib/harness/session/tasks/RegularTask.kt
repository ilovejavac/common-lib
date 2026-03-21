package com.dev.lib.harness.session.tasks

import com.dev.lib.None
import com.dev.lib.Option
import com.dev.lib.Outcome
import com.dev.lib.Some
import com.dev.lib.harness.protocol.*
import com.dev.lib.harness.session.AgentSession
import com.dev.lib.harness.session.SessionTask
import com.dev.lib.log

class RegularTask : SessionTask {
    override val kind: TaskKind = TaskKind.Regular

    override fun spanName(): String = "session_task.turn"

    override suspend fun run(
        context: SessionTaskContext, turn: TurnContext, inputs: List<UserInput>
    ): Option<String> {

        val session = context.session
        session.emit(
            EventMsg.TurnStarted(
                turn = turn.submissionId, modelContextWindow = turn.modelContextWindow()
            )
        )

        // 预热

        // 对话完整生命周期
        if (inputs.isEmpty()) {
            return None
        }

        // 1. 预处理上下文（压缩、技能/插件/连接器解析、埋点和历史记录）
        val modelInfo = turn.modelInfo
        val autoCompactTokenLimit = modelInfo.autoCompactTokenLimit
        // 在真正采样前做一次压缩，避免历史 token 超阈值导致后续请求失败。
        if (runSamplingCompact(session, turn).isErr()) {
            log.error("Failed to run sampling compact")
            return None
        }

        // 2. 循环向模型发起请求（可能触发工具调用并继续追问）

        // 3. 在满足收敛条件后执行 stop/after_agent

        // 4. 处理可恢复和不可恢复错误

        return Some("")
    }

    private fun runSamplingCompact(session: AgentSession, turnContext: TurnContext): Outcome<None, String> {


        return Outcome.Success(None)
    }
}

