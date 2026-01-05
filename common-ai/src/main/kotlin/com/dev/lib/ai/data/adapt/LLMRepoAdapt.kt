package com.dev.lib.ai.data.adapt

import com.dev.lib.ai.data.dao.AiModelConfigDao
import com.dev.lib.ai.repo.AiLLMRepo
import com.dev.lib.ai.service.llm.LLM
import com.dev.lib.orNull
import org.springframework.stereotype.Component

@Component
class LLMRepoAdapt(
    val dao: AiModelConfigDao
) : AiLLMRepo {
    override fun loadLLM(id: String): LLM? {
        val model = dao.load(AiModelConfigDao.Q().apply {
            bizId = id
        }).orNull()
        return model?.toLLM(80_000)
    }
}