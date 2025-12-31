package com.dev.lib.ai.service

import com.dev.lib.ai.service.agent.ReActAgent
import com.dev.lib.ai.service.agent.session.AiChatSession
import com.dev.lib.ai.service.agent.session.SessionInterceptor
import com.dev.lib.ai.trigger.request.AgentChatRequest
import com.dev.lib.entity.id.IDWorker
import com.dev.lib.security.util.SecurityContextHolder
import kotlinx.coroutines.CoroutineScope
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * ai 问答对话服务
 */
@Component
class AiService(
    val scope: CoroutineScope,
    val interceptors: List<SessionInterceptor>
) {

    fun call(task: String): String {
        TODO()
    }

    fun chat(cmd: AgentChatRequest.Chat): SseEmitter {
        for (interceptor in interceptors) {
            interceptor.perHandle(SecurityContextHolder.get(), cmd)
        }

        // 获取模型

        // 加载历史对话

        // 装填上下文
        val session = AiChatSession(
            cmd.session,
            scope,
            ReActAgent()
        )
        val chat = session.chat(cmd.content)
        chat.onCompletion {
            for (interceptor in interceptors) {
                interceptor.afterCompletion()
            }
        }
        return chat
    }

    fun opensession(): String {
        return IDWorker.newId()
    }
}