package com.dev.lib.ai.service

import com.dev.lib.ai.repo.AiModelRepo
import com.dev.lib.ai.trigger.dto.AiModelResponse
import com.dev.lib.ai.trigger.request.AiModelRequest
import com.dev.lib.web.model.QueryRequest
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

/**
 * 模型配置服务
 */
@Component
class ModelConfigService(
    val repo: AiModelRepo
) {

    fun handle(cmd: AiModelRequest.CreateModel): String {
        TODO()
    }

    fun handle(cmd: AiModelRequest.UpdateModel): String {
        TODO()
    }

    fun remove(id: String): String {
        TODO("Not yet implemented")
    }

    fun query(q: QueryRequest<AiModelRequest.QueryModel>): Page<List<AiModelResponse.AiModelDto>> {
        TODO("Not yet implemented")
    }
}