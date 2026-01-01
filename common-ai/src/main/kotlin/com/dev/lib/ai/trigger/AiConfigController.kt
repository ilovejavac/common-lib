package com.dev.lib.ai.trigger

import com.dev.lib.ai.service.ModelConfigService
import com.dev.lib.ai.trigger.dto.AiModelResponse
import com.dev.lib.ai.trigger.request.AiModelRequest
import com.dev.lib.web.model.QueryRequest
import com.dev.lib.web.model.ServerResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * 模型配置接口
 *
 * 负责租户模型配置 crud
 */
@RestController
class AiConfigController (
    val service: ModelConfigService
) {
    // 新增 ai 配置
    @PostMapping("/api/ai/model-create")
    fun createModel(@RequestBody @Validated cmd: AiModelRequest.CreateModel) =
        ServerResponse.success<String>(service.handle(cmd))

    // 修改 ai 配置
    @PutMapping("/api/ai/model-update")
    fun updateModel(@RequestBody @Validated cmd: AiModelRequest.UpdateModel) =
        ServerResponse.success<String>(service.handle(cmd))

    // 删除 ai 配置
    @DeleteMapping("/api/ai/model-delete/{id}")
    fun removeModel(@PathVariable id: String) =
        ServerResponse.success<String>(service.remove(id))

    // 查询 ai 配置
    @PostMapping("/api/ai/model-list")
    fun listModel(@RequestBody @Validated q: QueryRequest<AiModelRequest.QueryModel>) =
        ServerResponse.success<List<AiModelResponse.AiModelDto>>(service.query(q))
}