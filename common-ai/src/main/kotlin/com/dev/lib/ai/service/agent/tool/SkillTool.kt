package com.dev.lib.ai.service.agent.tool

/**
 * 加载 skill.md
 */
class SkillTool {

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