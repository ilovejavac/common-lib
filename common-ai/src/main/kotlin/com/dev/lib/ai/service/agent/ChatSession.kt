package com.dev.lib.ai.service.agent

import com.dev.lib.ai.model.AceItem
import com.dev.lib.ai.model.ChatItem
import com.dev.lib.ai.model.ChatResponse
import com.dev.lib.ai.model.ChatSSE
import com.dev.lib.ai.service.agent.tool.skill.SkillToolManager
import com.dev.lib.ai.service.llm.LLM

/**
 * 会话
 */
interface ChatSession {
    val sessionId: String

    val llm: LLM

    var skillToolManager: SkillToolManager
    /**
     * 全量记忆
     * */
    val history: MutableList<ChatItem>

    // ace
    val acePayload: MutableList<AceItem>

    var response: ChatResponse

    fun workingMemory(prompt: String): List<ChatItem>

    fun generate(prompt: String, block: () -> Unit): ChatSSE
}