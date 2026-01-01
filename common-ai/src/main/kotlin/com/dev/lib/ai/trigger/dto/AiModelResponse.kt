package com.dev.lib.ai.trigger.dto

import com.dev.lib.ai.model.ModelEndpoint
import com.dev.lib.web.BaseVO
import java.math.BigDecimal

class AiModelResponse {

    data class AiModelDto(
        var name: String? = null,
        var model: String,
        var endpoint: ModelEndpoint,
        var apiKey: String,

        var temperature: BigDecimal? = null,
        var topP: BigDecimal? = null,
        var topK: Int? = null,
        var maxTokens: Int? = null,

        var enabled: Boolean,
    ) : BaseVO()
}