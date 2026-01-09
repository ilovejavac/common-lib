package com.dev.lib.ai.service.agent.support.skill

import com.dev.lib.storage.domain.service.FileService

/**
 * 加载 skill.md
 */
class SkillTool (
    val fileService: FileService
) {

    fun loadSkill(skillName: String): String {

        val content = doReadSkill();
        return """
            <skill-loaded name="$skillName">
            $content
            </skill-loaded>
        """.trimIndent()
    }

    fun doReadSkill(): String {
        return ""
    }
}