package com.dev.lib.ai.data.entity

import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.*

/**
 * 完整会话历史
 */
@Entity
@Table(name = "sys_ai_session_history")
class AiSessionHistoryDo(
    @Column(name = "session_id")
    var sessionId: Long? = null,
    @ManyToOne
    @JoinColumn(name = "session_id")
    var session: AiSessionDo? = null,

    var role: String? = null,
    var content: String? = null,

    var documents: MutableList<String> = mutableListOf()
) : TenantEntity()