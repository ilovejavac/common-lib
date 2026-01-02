package com.dev.lib.ai.trigger.request

import com.dev.lib.ai.model.ModelEndpoint
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

class AiModelRequest {

    class CreateModel {
        @field:NotBlank(message = "模型名称不能为空")
        lateinit var name: String

        @field:NotBlank(message = "模型不能为空")
        lateinit var model: String

        @field:NotBlank(message = "模型访问 url 不能为空")
        lateinit var baseUrl: String

        @field:NotNull(message = "模型类型不能为空")
        lateinit var endpoint: ModelEndpoint

        @field:NotBlank(message = " apikey 不能为空")
        lateinit var apiKey: String

        var temperature: BigDecimal? = null
        var topP: BigDecimal? = null
        var topK: Int? = null
        var maxTokens: Int? = null
        var enabled: Boolean = true
    }

    data class UpdateModel(
        @field:JsonProperty("id")
        @field:NotBlank(message = "模型 id 不能为空")
        val bizId: String,
        var name: String? = null,
        @field:NotBlank(message = "模型不能为空")
        var model: String,
        @field:NotBlank(message = "模型访问 url 不能为空")
        var baseUrl: String,
        @field:NotNull(message = "模型类型不能为空")
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