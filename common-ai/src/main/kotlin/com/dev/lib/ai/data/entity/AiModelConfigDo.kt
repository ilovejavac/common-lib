package com.dev.lib.ai.data.entity

import com.dev.lib.ai.model.ModelEndpoint
import com.dev.lib.ai.service.llm.AiModelLangchain
import com.dev.lib.ai.service.llm.LLM
import com.dev.lib.ai.trigger.dto.AiModelResponse
import com.dev.lib.ai.trigger.request.AiModelRequest
import com.dev.lib.jpa.TenantEntity
import io.github.linpeilie.annotations.AutoMapper
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * 模型配置
 */
@Entity
@Table(name = "sys_ai_model")
@AutoMapper(target = AiModelResponse.AiModelDto::class, reverseConvertGenerate = false)
@AutoMapper(target = AiModelRequest.CreateModel::class, convertGenerate = false)
@AutoMapper(target = AiModelRequest.UpdateModel::class, convertGenerate = false)
data class AiModelConfigDo(
    var name: String,
    var model: String,
    var baseUrl: String,

    @Column(length = 12)
    @Enumerated(EnumType.STRING)
    var endpoint: ModelEndpoint,

    var apiKey: String,
) : TenantEntity() {

    var temperature: BigDecimal? = null
    var topP: BigDecimal? = null
    var topK: Int? = null
    var maxTokens: Int? = 130_000

    var enabled: Boolean = true

    @OneToMany(mappedBy = "model")
    val sessions: MutableList<AiSessionDo> = mutableListOf()

    fun clearSession() {
        sessions.forEach {
            it.modelId = null
        }
        sessions.clear()
    }

    fun toLLM(tokenLimit: Int): LLM {
        return AiModelLangchain(
            model = model,
            endpoint = endpoint,
            apiKey = apiKey,
            temperature = temperature,
            topP = topP,
            baseUrl = baseUrl,
            maxTokens = maxTokens ?: tokenLimit
        )
    }
}