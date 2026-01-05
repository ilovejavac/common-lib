package com.dev.lib.ai.service.agent.session

import com.dev.lib.ai.model.AiAgentErrorCode
import com.dev.lib.ai.model.AiAgentException
import com.dev.lib.ai.service.agent.ChatSession
import com.dev.lib.ai.trigger.request.AgentChatRequest
import com.dev.lib.security.util.UserDetails
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 并发 chat 限制
 */
@Order(0)
@Component
class ConcurrentChatInterceptor(

) : SessionInterceptor {

    private val chatLock = ConcurrentHashMap<String, AtomicBoolean>()

    fun tryAcquire(session: String): Boolean =
        chatLock.computeIfAbsent(session) { AtomicBoolean(false) }
            .compareAndSet(false, true)

    fun release(session: String) {
        chatLock[session]?.set(false)
    }

    override fun perHandle(
        userDetails: UserDetails?,
        request: AgentChatRequest.Chat
    ) {
        if (!tryAcquire(request.session)) {
            throw AiAgentException(AiAgentErrorCode.CONCURRENT_CHAT_ERROR)
        }
    }

    override fun afterCompletion(session: ChatSession) {
        release(session.sessionId)
    }
}