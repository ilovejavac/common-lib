package com.dev.lib.ai.service.agent

import com.dev.lib.ai.model.ChatSSE

fun interface Agent {

    // system, model, messages, tools

    suspend fun run(prompt: String, session: ChatSession, sse: ChatSSE)

}