package com.dev.lib.ai.service

import com.dev.lib.ai.repo.AiLLMRepo
import com.dev.lib.ai.repo.AiSessionStore
import com.dev.lib.ai.service.agent.session.SessionInterceptor
import com.dev.lib.ai.service.agent.tool.skill.SkillToolManager
import com.dev.lib.ai.trigger.request.AgentChatRequest
import com.dev.lib.log
import com.dev.lib.security.util.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * ai 问答对话服务
 */
@Component
class AiService(
    val store: AiSessionStore,
    val llmRepo: AiLLMRepo,
    val skillToolManager: SkillToolManager,
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

        val session = store.loadSession(cmd.session, cmd.model?.let {
            llmRepo.loadLLM(it)
        })
        session.skillToolManager = skillToolManager

        return session.generate(cmd.prompt) {
            for (interceptor in interceptors) {
                try {
                    interceptor.afterCompletion(session)
                } catch (e: Exception) {
                    log.warn("session 后置拦截器执行异常", e)
                }
            }
        }
    }

    fun opensession(): String {
        return store.openSession().sessionId
    }
}
