package com.dev.lib.ai.trigger

import com.dev.lib.ai.service.ModelConfigService
import com.dev.lib.ai.trigger.request.AiModelRequest
import com.dev.lib.web.model.QueryRequest
import com.dev.lib.web.model.ServerResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * 模型配置接口
 *
 * 负责租户模型配置 crud
 */
@RestController
class AiConfigController(
    val service: ModelConfigService
) {
    // 新增 ai 配置
    @PostMapping("/api/ai/model-create")
    fun createModel(@RequestBody @Validated cmd: AiModelRequest.CreateModel) =
        ServerResponse.success<String>(service.handle(cmd))

    // 修改 ai 配置
    @PutMapping("/api/ai/model-update")
    fun updateModel(@RequestBody @Validated cmd: AiModelRequest.UpdateModel): ServerResponse<Unit> {
        service.handle(cmd)
        return ServerResponse.ok()
    }


    // 删除 ai 配置
    @DeleteMapping("/api/ai/model-delete/{id}")
    fun removeModel(@PathVariable id: String): ServerResponse<Unit> {
        service.remove(id)

        return ServerResponse.ok()
    }

    // 查询 ai 配置
    @PostMapping("/api/ai/model-list")
    fun listModel(@RequestBody @Validated q: QueryRequest<AiModelRequest.QueryModel>) =
        ServerResponse.success(service.query(q))
}