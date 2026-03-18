package com.dev.lib.harness.sdk.skill

class SkillManager {
    private lateinit var storage: SkillStorage


}

interface SkillStorage {
    fun loadSkills(skills: Collection<String>): Map<String, SkillMeta>

}