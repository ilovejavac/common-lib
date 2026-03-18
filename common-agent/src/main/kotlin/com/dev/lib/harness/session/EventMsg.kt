package com.dev.lib.harness.session

sealed interface EventMsg {
    data class UserMessageEvent(
        val message: String
    ) : EventMsg

    data class AgentMessageDeltaEvent(
        val message: String
    ) : EventMsg

    data class ListSkillsResponseEvent(
        val skills: List<String>
    ) : EventMsg
}