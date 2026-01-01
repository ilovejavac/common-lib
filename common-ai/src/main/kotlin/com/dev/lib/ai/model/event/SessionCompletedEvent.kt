package com.dev.lib.ai.model.event

import com.dev.lib.ai.service.agent.ChatSession
import com.dev.lib.domain.DomainEvent

data class SessionCompletedEvent(
    val session: ChatSession
) : DomainEvent("SessionCompletedEvent", 1) {
}
