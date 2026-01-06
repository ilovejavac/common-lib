package com.dev.lib.ai.service.agent.tool.skill

import com.dev.lib.ai.repo.AiSkillRepo
import org.springframework.stereotype.Component

@Component
class SkillToolManager(
    val repo: AiSkillRepo
) {



    fun to_prompt(): String {
        val metadata = repo.loadSkillMetadata()

        metadata.forEach {
            println(it)
        }
        return """
            
        """.trimIndent()
    }
}