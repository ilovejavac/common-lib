package com.dev.lib.ai.data.entity

import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.*

@Entity
@Table(name = "sys_ai_document_item")
class AiDocumentChunkDo(
    @Column(name = "document_id")
    var documentId: Long? = null,
    @ManyToOne
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    var document: AiDocumentDo? = null,

    var chunkIndex: Int? = null,

    // length(450)
    var content: String? = null,
    var contentHash: String?
) : TenantEntity() {
}