package com.dev.lib.ai.service

import com.dev.lib.ai.repo.AiSessionStore
import com.dev.lib.ai.service.agent.session.SessionInterceptor
import com.dev.lib.ai.trigger.request.AgentChatRequest
import com.dev.lib.security.util.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * ai 问答对话服务
 */
@Component
class AiService(
    val store: AiSessionStore,
    val interceptors: List<SessionInterceptor>
) {

    fun call(task: String): String {
        TODO()
    }

    fun chat(cmd: AgentChatRequest.Chat): SseEmitter {
        // 前置过滤
        // eg: vip，敏感词
        for (interceptor in interceptors) {
            interceptor.perHandle(SecurityContextHolder.get(), cmd)
        }

        // 加载历史对话 session(history,ace)
        val session = store.loadSession(cmd.session)

        return session.generate(cmd.prompt)
    }

    fun opensession(): String {
        return store.openSession().sessionId
    }
}