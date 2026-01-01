package com.dev.lib.ai.data.entity

import com.dev.lib.ai.model.ChatMessage
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

    var role: String,
    var content: String,

    var inputToken: Int = 0,
    var outputToken: Int = 0,

    var documents: MutableList<String> = mutableListOf()
) : TenantEntity() {
    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            role = role,
            content = content,
            input = inputToken,
            output = outputToken
        )
    }
}