package com.dev.lib.ai.service.agent

import com.dev.lib.ai.model.ChatSSE

/**
 * ReAct流程
 */
fun interface Agent {

    fun run(task: String, session: ChatSession, sse: ChatSSE)
}