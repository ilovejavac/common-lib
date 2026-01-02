package com.dev.lib.ai.data.entity

import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.*

@Entity
@Table(name = "sys_ai_document_item")
class AiDocumentChunkDo(
    @Column(name = "document_id")
    var documentId: Long?,


    var chunkIndex: Int = 0,

    // length(450)
    var content: String,
    var contentHash: String
) : TenantEntity() {

    @ManyToOne
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    lateinit var document: AiDocumentDo
}