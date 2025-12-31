package com.dev.lib.ai.trigger.dto

import com.dev.lib.web.BaseVO

class AgentChatResponse {

    data class SessionDto(
        val name: String,
    ) : BaseVO()
}