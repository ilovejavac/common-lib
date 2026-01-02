package com.dev.lib.ai.data.adapt

import com.dev.lib.ai.data.dao.AiModelConfigDao
import com.dev.lib.ai.data.entity.`AiModelConfigDoToAiModelResponse$AiModelDtoMapper`
import com.dev.lib.ai.repo.AiModelRepo
import com.dev.lib.ai.trigger.dto.AiModelResponse
import com.dev.lib.ai.trigger.request.AiModelRequest
import com.dev.lib.ai.trigger.request.`AiModelRequest$CreateModelToAiModelConfigDoMapper`
import com.dev.lib.ai.trigger.request.`AiModelRequest$UpdateModelToAiModelConfigDoMapper`
import com.dev.lib.web.model.QueryRequest
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AiModelAdapt(
    val dao: AiModelConfigDao,

    val createConvert: `AiModelRequest$CreateModelToAiModelConfigDoMapper`,
    val updateConvert: `AiModelRequest$UpdateModelToAiModelConfigDoMapper`,
    val convert: `AiModelConfigDoToAiModelResponse$AiModelDtoMapper`
) : AiModelRepo {

    override fun addModel(cmd: AiModelRequest.CreateModel): AiModelResponse.AiModelDto {
        val model = dao.save(createConvert.convert(cmd))
        return convert.convert(model)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun updateModel(cmd: AiModelRequest.UpdateModel) {
        dao.load(AiModelConfigDao.Q().setBizId(cmd.bizId)).ifPresent {
            updateConvert.convert(cmd, it)
        }
    }

    override fun deleteModel(id: String) {
        dao.delete(AiModelConfigDao.Q().setBizId(id))
    }

    @Transactional(readOnly = true)
    override fun listModel(query: QueryRequest<AiModelRequest.QueryModel>): Page<AiModelResponse.AiModelDto> {
        return dao.page(AiModelConfigDao.Q().external(query))
            .map {
                convert.convert(it)
            }
    }
}