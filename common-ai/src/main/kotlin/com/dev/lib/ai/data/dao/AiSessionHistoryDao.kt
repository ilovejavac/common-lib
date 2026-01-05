package com.dev.lib.ai.data.dao

import com.dev.lib.ai.data.entity.AiSessionHistoryDo
import com.dev.lib.entity.dsl.DslQuery
import com.dev.lib.jpa.entity.BaseRepository

interface AiSessionHistoryDao : BaseRepository<AiSessionHistoryDo> {
    class Q : DslQuery<AiSessionHistoryDo>() {
        var sessionId: Long = 0
    }
}