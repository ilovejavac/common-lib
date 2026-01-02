package com.dev.lib.ai.repo

import com.dev.lib.ai.service.llm.LLM

interface AiLLMRepo {
    fun loadLLM(id: String): LLM?
}