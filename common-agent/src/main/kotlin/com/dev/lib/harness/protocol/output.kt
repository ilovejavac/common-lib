package com.dev.lib.harness.protocol

import com.dev.lib.notify.model.Message
import com.dev.lib.util.Jsons

sealed class EventMsg(val type: String) {

    data class Error(
        val message: String
    ) : EventMsg("error")

    data class TurnStarted(
        val turn: Turn
    ) : EventMsg("turn/started")

    data class TurnCompleted(
        val turn: Turn
    ) : EventMsg("turn/completed")

    data class TurnPlanUpdated(
        val turn: String,
        val plan: List<TurnPlanStep>
    ) : EventMsg("turn/plan/updated")

    data class AgentMessageDelta(
        val turn: String,
        val delta: String
    ) : EventMsg("item/agent-message/delta")

    data class FileChangeOutputDelta(
        val turn: String,
        val delta: String
    ) : EventMsg("item/file-change/output-delta")


}

data class EventMessage(val em: EventMsg) : Message() {
    override fun getData(): EventMsg = em
}

fun main() {
    val em: EventMsg = EventMsg.AgentMessageDelta("1", "xxx")

    print(Jsons.toJson(em))
}