package com.dev.lib.harness.sdk.model

import org.springframework.ai.chat.model.ChatModel

interface LlmClient {
    fun getChatModel(): ChatModel
}