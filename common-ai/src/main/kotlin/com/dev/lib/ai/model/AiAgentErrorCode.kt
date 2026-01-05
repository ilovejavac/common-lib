package com.dev.lib.ai.model

import com.dev.lib.web.model.CodeEnums

enum class AiAgentErrorCode(
    val code: Int,
    val msg: String
) : CodeEnums {
    CONCURRENT_CHAT_ERROR(1010, "并发访问冲突");

    override fun getCode(): Int = this.code

    override fun getMessage(): String = this.msg
}