package com.dev.lib.harness.session

import com.dev.lib.harness.sdk.model.ModelProvider
import com.dev.lib.harness.sdk.skill.SkillManager
import org.springframework.stereotype.Component

class SessionManager(
    val sessionStorage: SessionStorage,
    val modelProvider: ModelProvider,
    val skillManager: SkillManager,
) {

    fun openSession(id: String): AgentSession {

        return AgentSession.newBuilder()
            .modelProvider(modelProvider)
            .build()
    }

}