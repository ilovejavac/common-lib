package com.dev.lib.ai.model

enum class ModelEndpoint(
    val provider: String,
    val path: String
) {
    OPENAI("openai", "/v1"),
    ANTHROPIC("Anthropic", "/v1"),
    ;
}