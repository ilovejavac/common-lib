package com.dev.lib.ai.data.dao

import com.dev.lib.ai.data.entity.AiSessionDo
import com.dev.lib.entity.dsl.DslQuery
import com.dev.lib.jpa.entity.BaseRepository

interface AiSessionDao : BaseRepository<AiSessionDo> {
    class Q : DslQuery<AiSessionDo>() {
    }
}