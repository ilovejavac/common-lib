package com.dev.lib.ai.trigger.request

import com.dev.lib.ai.model.ModelEndpoint
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

class AiModelRequest {

    data class CreateModel(
        @field:NotBlank(message = "模型名称不能为空")
        var name: String,
        @field:NotBlank(message = "模型不能为空")
        var model: String,
        @field:NotBlank(message = "模型类型不能为空")
        var endpoint: ModelEndpoint,
        @field:NotBlank(message = " apikey 不能为空")
        var apiKey: String,

        var temperature: BigDecimal? = null,
        var topP: BigDecimal? = null,
        var topK: Int? = null,
        var maxTokens: Int? = null,

        var enabled: Boolean = true,
    )

    data class UpdateModel(
        @field:JsonProperty("id")
        @field:NotBlank(message = "模型 id 不能为空")
        val bizId: String,
        var name: String? = null,
        @field:NotBlank(message = "模型不能为空")
        var model: String,
        @field:NotBlank(message = "模型类型不能为空")
        var endpoint: ModelEndpoint,
        @field:NotBlank(message = " apikey 不能为空")
        var apiKey: String,

        var temperature: BigDecimal? = null,
        var topP: BigDecimal? = null,
        var topK: Int? = null,
        var maxTokens: Int? = null,

        var enabled: Boolean?,
    )

    data class QueryModel(
        val name: String? = null,
        val endpoint: ModelEndpoint? = null
    )
}