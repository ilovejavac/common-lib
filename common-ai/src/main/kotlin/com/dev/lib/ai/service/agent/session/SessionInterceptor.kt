package com.dev.lib.ai.service.agent.session

import com.dev.lib.ai.service.agent.ChatSession
import com.dev.lib.ai.trigger.request.AgentChatRequest
import com.dev.lib.security.util.UserDetails

interface SessionInterceptor {
    fun perHandle(userDetails: UserDetails?, request: AgentChatRequest.Chat) {

    }

    fun afterCompletion(session: ChatSession) {

    }
}