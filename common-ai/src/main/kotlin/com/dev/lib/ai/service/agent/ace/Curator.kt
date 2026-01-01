package com.dev.lib.ai.service.agent.ace

import com.dev.lib.ai.service.agent.ChatSession
import com.dev.lib.ai.service.llm.LLM

/**
 * 管理演化上下文
 */
class Curator(
    val llm: LLM,
) {


    val generator = Generator()
    val reflector = Reflector()

    fun run(
        session: ChatSession
    ) {

        // 反思，评估
        generator
        // 上下文演化
        reflector

    }
}