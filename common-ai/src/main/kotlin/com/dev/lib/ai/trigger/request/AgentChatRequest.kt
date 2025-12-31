package com.dev.lib.ai.trigger.request

import jakarta.validation.constraints.NotBlank

class AgentChatRequest {

    data class Chat(
        @field:NotBlank(message = "会话 id 不能为空")
        val session: String,
        @field:NotBlank(message = "模型 id 不能为空")
        val model: String,
        @field:NotBlank(message = "对话内容不能为空")
        val content: String,
        val documents: List<String> = listOf()
    )
}