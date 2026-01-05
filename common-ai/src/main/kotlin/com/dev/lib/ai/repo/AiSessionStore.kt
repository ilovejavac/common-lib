package com.dev.lib.ai.repo

import com.dev.lib.ai.service.agent.ChatSession
import com.dev.lib.ai.service.llm.LLM

/**
 * 持久化上下文接口
 */
interface AiSessionStore {

    fun openSession(): ChatSession

    fun loadSession(id: String, llm: LLM? = null): ChatSession

    fun storeSessionHistory(session: ChatSession)
}