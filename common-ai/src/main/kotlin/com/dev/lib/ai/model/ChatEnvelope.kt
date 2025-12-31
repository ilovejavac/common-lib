package com.dev.lib.ai.model

data class ChatEnvelope<T>(
    val payload: T
) {
}