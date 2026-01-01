package com.dev.lib.ai.data.entity

import com.dev.lib.ai.model.ModelEndpoint
import com.dev.lib.ai.service.llm.LLM
import com.dev.lib.ai.service.llm.LangchainModel
import com.dev.lib.ai.trigger.dto.AiModelResponse
import com.dev.lib.ai.trigger.request.AiModelRequest
import com.dev.lib.jpa.TenantEntity
import io.github.linpeilie.annotations.AutoMapper
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * 模型配置
 */
@Entity
@Table(name = "sys_ai_model")
@AutoMapper(target = AiModelResponse.AiModelDto::class, reverseConvertGenerate = false)
@AutoMapper(target = AiModelRequest.CreateModel::class, convertGenerate = false)
@AutoMapper(target = AiModelRequest.UpdateModel::class, convertGenerate = false)
class AiModelConfigDo(
    var name: String? = null,
    var model: String,
    var endpoint: ModelEndpoint,
    var apiKey: String,

    var temperature: BigDecimal? = null,
    var topP: BigDecimal? = null,
    var topK: Int? = null,
    var maxTokens: Int? = null,

    var enabled: Boolean = true,

    @OneToMany(mappedBy = "model")
    var sessions: MutableList<AiSessionDo> = mutableListOf()
) : TenantEntity() {
    fun clearSession() {
        sessions.forEach {
            it.modelId = null
        }
        sessions.clear()
    }

    fun toLLM(): LLM {
        return LangchainModel(
            model = model,
            endpoint = endpoint,
            apiKey = apiKey,
            temperature = temperature,
            topP = topP,
            topK = topK,
            maxTokens = maxTokens
        )
    }
}