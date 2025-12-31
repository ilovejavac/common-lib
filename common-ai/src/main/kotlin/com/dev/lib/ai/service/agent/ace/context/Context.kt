package com.dev.lib.ai.service.agent.ace.context

/**
 * 上下文
 */
data class Context(
    val session: String,
    val limit: Int = 50000,
    /**
     * 当前 session 对话总 token 超过阈值后对前 50% 的内容进行归纳总结
     */
    val threshold: Float = 0.95f
) {
    var tokens: Int = 0
    var coast: Long = 0

    /**
     * 归纳整理后的当前消息
     * */
    val message: List<Any> = ArrayList()
    /**
     * 全量记忆
     * */
    val history: List<Any> = ArrayList()
}