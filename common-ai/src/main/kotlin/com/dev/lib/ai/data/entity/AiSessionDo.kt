package com.dev.lib.ai.data.entity

import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal

/**
 * 会话摘要 context，会话名称
 */
@Entity
@Table(name = "sys_ai_agent_session")
class AiSessionDo(
    var name: String? = null,
    var description: String? = null,

    @JoinColumn(name = "model_id")
    var modelId: Long? = null,
    @ManyToOne
    @JoinColumn(name = "model_id", insertable = false, updatable = false)
    var model: AiAgentConfigDo? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    var summary: String? = null,

    var tokens: Int = 0,
    var tokenLimit: Int = 50000,
    var threshold: BigDecimal = BigDecimal("0.95"),

    @OneToMany(mappedBy = "session", cascade = [CascadeType.ALL], orphanRemoval = true)
    var histories: MutableList<AiSessionHistoryDo> = mutableListOf()
) : TenantEntity() {

    fun addContent(content: AiSessionHistoryDo) {
        content.sessionId = id
        histories.add(content)
    }

    fun setAiModel(model: AiAgentConfigDo) {
        modelId = model.id
        this.model = model
    }

}