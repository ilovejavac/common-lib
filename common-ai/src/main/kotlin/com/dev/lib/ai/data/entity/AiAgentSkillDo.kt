package com.dev.lib.ai.data.entity

import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "sys_ai_agent_skill")
data class AiAgentSkillDo(

    @Column(length = 64)
    var name: String,

    @Column(length = 1024, name = "skill_description")
    var description: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    var contentBody: String,
) : TenantEntity() {

    var license: String? = null

    @Column(length = 500)
    var compatibility: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text", name = "skill_metadata")
    var metadata: Map<String, String> = mapOf()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text", name = "skill_scripts")
    val scripts: MutableList<String> = mutableListOf()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text", name = "skill_assets")
    val assets: MutableList<String> = mutableListOf()

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text", name = "skill_references")
    val references: MutableList<String> = mutableListOf()
}