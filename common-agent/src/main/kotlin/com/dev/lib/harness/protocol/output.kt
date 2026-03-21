package com.dev.lib.harness.protocol

import com.dev.lib.Option
import com.dev.lib.harness.HarnessError
import com.dev.lib.notify.model.Message

sealed class EventMsg(val type: String) {

    data class Error(
        val message: String
    ) : EventMsg("error")

    data class Warning(
        val message: String
    ) : EventMsg("warning") {

        constructor(harnessError: HarnessError) : this(harnessError.message)
    }

    data class TurnStarted(
        val turn: String, val modelContextWindow: Int
    ) : EventMsg("turn/started")

    data class TurnCompleted(
        val turn: String, val lastAgentMessage: Option<String>
    ) : EventMsg("turn/completed")

    data class TurnAbort(
        val turn: String, val reason: TurnAbortReason
    ) : EventMsg("turn/aborted")

    data class TokenCount(
        val modelContextWindow: Int
    ) : EventMsg("token/count")

    data class AgentMessage(
        val message: String
    ) : EventMsg("item/agent-message")

    data class UserMessage(
        val message: String
    ) : EventMsg("item/user-message")

    data class AgentMessageDelta(
        val delta: String
    ) : EventMsg("item/agent-message/delta")

    data class ListSkillResponse(
        val skills: List<String>
    ) : EventMsg("skills")
}

data class EventMessage(val em: EventMsg) : Message() {
    override fun getData(): EventMsg = em
}