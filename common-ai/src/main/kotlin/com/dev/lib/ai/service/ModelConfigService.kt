package com.dev.lib.ai.service

import com.dev.lib.ai.repo.AiModelRepo
import com.dev.lib.ai.trigger.dto.AiModelResponse
import com.dev.lib.ai.trigger.request.AiModelRequest
import com.dev.lib.log
import com.dev.lib.web.model.QueryRequest
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

/**
 * 模型配置服务
 */
@Component
class ModelConfigService(
    val repo: AiModelRepo,

    ) {

    fun handle(cmd: AiModelRequest.CreateModel): String {
        log.info("cmd {}", cmd)
        return repo.addModel(cmd).bizId
    }

    fun handle(cmd: AiModelRequest.UpdateModel) {
        repo.updateModel(cmd)
    }

    fun remove(id: String) {
        repo.deleteModel(id)
    }

    fun query(q: QueryRequest<AiModelRequest.QueryModel>): Page<AiModelResponse.AiModelDto> {
        return repo.listModel(q)
    }
}