package com.dev.lib.ai.repo

import com.dev.lib.ai.trigger.dto.AiModelResponse
import com.dev.lib.ai.trigger.request.AiModelRequest
import com.dev.lib.web.model.QueryRequest
import org.springframework.data.domain.Page

interface AiModelRepo {
    fun addModel(cmd: AiModelRequest.CreateModel): AiModelResponse.AiModelDto

    fun updateModel(cmd: AiModelRequest.UpdateModel)

    fun deleteModel(id: String)

    fun listModel(query: QueryRequest<AiModelRequest.QueryModel>): Page<AiModelResponse.AiModelDto>
}