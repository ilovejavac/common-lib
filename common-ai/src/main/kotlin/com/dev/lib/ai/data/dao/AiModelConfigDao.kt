package com.dev.lib.ai.data.dao

import com.dev.lib.ai.data.entity.AiModelConfigDo
import com.dev.lib.entity.dsl.DslQuery
import com.dev.lib.jpa.entity.BaseRepository

interface AiModelConfigDao : BaseRepository<AiModelConfigDo> {
    class Q : DslQuery<AiModelConfigDo>() {

    }
}