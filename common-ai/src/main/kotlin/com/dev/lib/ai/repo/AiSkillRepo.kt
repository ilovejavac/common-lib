package com.dev.lib.ai.repo

interface AiSkillRepo {
    fun loadSkillMetadata(): List<SkillMetadata>
}

data class SkillMetadata(
    val name: String = "",
    val description: String = ""
)