package com.dev.lib.ai.trigger.request

import jakarta.validation.constraints.NotBlank

class AgentChatRequest {

    data class Chat(
        /**
         * 会话 id
         */
        @field:NotBlank(message = "会话 id 不能为空")
        val session: String,

        /**
         * 模型 id
         */
        val model: String? = null,

        /**
         * 用户输入提示词
         */
        @field:NotBlank(message = "对话内容不能为空")
        val prompt: String,

        /**
         * 选择知识库文献 id
         */
        val documents: List<String> = listOf()
    )
}