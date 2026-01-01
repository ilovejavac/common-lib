package com.dev.lib.ai.trigger.request

class DocumentRequest {

    data class ResolveDocument(
        val file: String
    )

    data class SaveDocument(
        val name: String,
        val icon: String,
    )
}