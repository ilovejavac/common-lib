package com.dev.lib.ai.data.entity

import com.dev.lib.ai.model.ChatItem
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
    @Column(columnDefinition = "text")
    var content: String,

    @Column(name = "chat_role")
    @Enumerated(EnumType.STRING)
    val role: ChatRole
) : TenantEntity() {

    @Column(name = "session_id", nullable = false)
    var sessionId: Long? = 0

    var tokenUsage: Int = 0

    @ManyToOne
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    lateinit var session: AiSessionDo

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    var documents: MutableList<String> = mutableListOf()

    fun toChatMessage(): ChatItem {
        return when (role) {
            ChatRole.USER -> ChatItem.user(content)
            ChatRole.ASSISTANT -> ChatItem.assistant(content)
            ChatRole.SYSTEM -> ChatItem.system(content)
        }.apply {
            token = tokenUsage
        }
    }
}