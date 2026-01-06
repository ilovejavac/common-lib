package com.dev.lib.ai.data.adapt

import com.dev.lib.ai.data.dao.AiAgentSkillDao
import com.dev.lib.ai.repo.AiSkillRepo
import com.dev.lib.ai.repo.SkillMetadata
import org.springframework.stereotype.Component

@Component
class AiSkillStore(
    val dao: AiAgentSkillDao
) : AiSkillRepo {
    override fun loadSkillMetadata(): List<SkillMetadata> {
        return dao.select("name", "description")
            .loads(SkillMetadata::class.java, AiAgentSkillDao.Q())
    }
}