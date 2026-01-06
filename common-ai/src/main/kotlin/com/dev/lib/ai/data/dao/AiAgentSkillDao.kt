package com.dev.lib.ai.data.dao

import com.dev.lib.ai.data.entity.AiAgentSkillDo
import com.dev.lib.entity.dsl.DslQuery
import com.dev.lib.jpa.entity.BaseRepository

interface AiAgentSkillDao : BaseRepository<AiAgentSkillDo> {

    class Q : DslQuery<AiAgentSkillDo>() {

    }
}