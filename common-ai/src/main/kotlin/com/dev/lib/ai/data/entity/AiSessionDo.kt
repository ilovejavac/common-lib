package com.dev.lib.ai.data.entity

import com.dev.lib.ai.model.AceItem
import com.dev.lib.ai.model.SessionKeyPoint
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
data class AiSessionDo(
    var name: String? = null
) : TenantEntity() {

    var description: String? = null

    @Column(name = "model_id")
    var modelId: Long? = null

    @ManyToOne
    @JoinColumn(name = "model_id", insertable = false, updatable = false)
    var model: AiModelConfigDo? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    var summary: String? = null

    var tokens: Int = 0
    var tokenLimit: Int = 80_000
    var threshold: BigDecimal = BigDecimal("0.85")

    @OneToMany(mappedBy = "session", cascade = [CascadeType.REMOVE], orphanRemoval = true)
    var histories: MutableList<AiSessionHistoryDo> = mutableListOf()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    var acePayloads: MutableList<AceItem> = mutableListOf()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    var keyPoints: MutableList<SessionKeyPoint> = mutableListOf()

    fun setContent(content: AiSessionHistoryDo): AiSessionHistoryDo {
        content.sessionId = id!!
        return content
    }

    fun setAiModel(model: AiModelConfigDo) {
        modelId = model.id
        this.model = model
    }

}