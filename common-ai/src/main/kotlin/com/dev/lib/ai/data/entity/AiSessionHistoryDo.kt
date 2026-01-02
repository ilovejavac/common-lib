package com.dev.lib.ai.data.entity

import com.dev.lib.ai.model.ChatMessage
import com.dev.lib.ai.model.ChatRole
import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * 完整会话历史
 */
@Entity
@Table(name = "sys_ai_session_history")
class AiSessionHistoryDo(
    @Column(name = "session_id", nullable = false)
    var sessionId: Long = 0,

    @Column(length = 12)
    @Enumerated(EnumType.STRING)
    var role: ChatRole,

    @Column(columnDefinition = "text")
    var content: String
) : TenantEntity() {
    @ManyToOne
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    lateinit var session: AiSessionDo

    var inputToken: Int = 0
    var outputToken: Int = 0
    var totalToken: Int = 0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    var documents: MutableList<String> = mutableListOf()

    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            role = role,
            content = content,
            input = inputToken,
            output = outputToken
        )
    }
}