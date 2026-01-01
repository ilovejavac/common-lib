package com.dev.lib.ai.service.agent

import com.dev.lib.ai.model.ChatSSE

fun interface Agent {

    suspend fun run(prompt: String, session: ChatSession, sse: ChatSSE)

}