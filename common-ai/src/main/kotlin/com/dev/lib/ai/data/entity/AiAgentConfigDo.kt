package com.dev.lib.ai.data.entity

import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * 模型配置
 */
@Entity
@Table(name = "sys_ai_model")
class AiAgentConfigDo(
    var name: String? = null,
    var provider: String,
    var model: String,
    var endpoint: String,
    var requestPath: String,
    var apiKey: String,

    var temperature: BigDecimal? = null,
    var topP: BigDecimal? = null,
    var topK: Int? = null,
    var maxTokens: Int? = null,

    var enabled: Boolean = true,

    @OneToMany(mappedBy = "model")
    var sessions: MutableList<AiSessionDo> = mutableListOf()
) : TenantEntity() {
    fun clearSession() {
        sessions.forEach {
            it.modelId = null
        }
        sessions.clear()
    }
}