package com.dev.lib.ai.data.entity

import com.dev.lib.jpa.TenantEntity
import jakarta.persistence.*

/**
 * 知识库
 */
@Entity
@Table(name = "sys_ai_document")
class AiDocumentDo(
    var name: String,
    var description: String? = null,

    //    文件类型
    var docType: String,

    //    源文件 id
    @Column(length = 12)
    var source: String
) : TenantEntity() {

    @OneToMany(mappedBy = "document", cascade = [CascadeType.ALL], orphanRemoval = true)
    var chunks: MutableList<AiDocumentChunkDo> = mutableListOf()

    fun addChunk(chunk: AiDocumentChunkDo) {
        chunk.documentId = id
        chunks.add(chunk)
    }

    fun removeChunk(chunk: AiDocumentChunkDo) {
        chunks.remove(chunk)
        chunk.documentId = null
    }

    fun clearChunks() {
        chunks.forEach {
            it.documentId = null
        }
        chunks.clear()
    }
}