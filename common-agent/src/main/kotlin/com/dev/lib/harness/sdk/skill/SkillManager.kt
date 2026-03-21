package com.dev.lib.harness.sdk.skill

import org.springframework.stereotype.Component

data class SkillManager(
    val storage: SkillStorage
) {


}

interface SkillStorage {
    fun loadSkills(skills: Collection<String>): Map<String, SkillMeta>

}