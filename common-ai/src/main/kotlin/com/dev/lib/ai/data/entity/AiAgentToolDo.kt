package com.dev.lib.ai.data.entity

import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * agent 工具
 */
@Entity
@Table(name = "sys_ai_agent_tool")
class AiAgentToolDo(
    var name: String? = null,
    var description: String? = null,

    ) : TenantEntity() {
}