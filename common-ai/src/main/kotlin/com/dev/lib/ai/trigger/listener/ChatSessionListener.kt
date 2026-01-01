package com.dev.lib.ai.trigger.listener

import com.dev.lib.ai.model.event.SessionCompletedEvent
import com.dev.lib.ai.repo.AiSessionStore
import com.dev.lib.ai.service.agent.ace.Curator
import com.dev.lib.log
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ChatSessionListener(
    val store: AiSessionStore
) {

    @EventListener
    fun storeSessionOnSessionCompleted(event: SessionCompletedEvent) {
        log.info("持久化 session")
        store.storeSession(event.session)
    }

    @EventListener
    fun aceOnSessionCompleted(event: SessionCompletedEvent) {
        log.info("开始 ACE 反思优化")
        Curator(event.session.llm).run(event.session)
    }

    // 总结上下文关键点
}