package com.dev.lib.ai.data.adapt

import com.dev.lib.ai.data.dao.AiSessionDao
import com.dev.lib.ai.data.entity.AiSessionDo
import com.dev.lib.ai.data.entity.AiSessionHistoryDo
import com.dev.lib.ai.model.AceItem
import com.dev.lib.ai.model.ChatMessage
import com.dev.lib.ai.model.ChatResponse
import com.dev.lib.ai.model.ChatSSE
import com.dev.lib.ai.repo.AiSessionStore
import com.dev.lib.ai.service.agent.AiAgent
import com.dev.lib.ai.service.agent.AiChatSession
import com.dev.lib.ai.service.agent.ChatSession
import com.dev.lib.ai.service.llm.LLM
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SessionStorage(
    val dao: AiSessionDao
) : AiSessionStore {

    override fun openSession(): ChatSession {
        val session = dao.save(
            AiSessionDo(
                name = "未命名"
            )
        )

        return OfflineSession(session.bizId)
    }

    @Transactional(readOnly = true)
    override fun loadSession(id: String, llm: LLM?): ChatSession {
        val session = dao.load(AiSessionDao.Q().setBizId(id))
            .orElseThrow { IllegalArgumentException("会话不存在: $id") }

        val model = llm
            ?: session.model?.toLLM()
            ?: throw IllegalStateException("未关联有效LLM模型")

        return AiChatSession(
            sessionId = id,
            llm = model,
            agent = AiAgent(),
            history = session.toChatMessage(),
            acePayload = session.acePayloads
        )
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun storeSession(session: ChatSession) {
        val d = dao.load(AiSessionDao.Q().setBizId(session.sessionId)).orElseThrow()

        session.history.takeLast(2).map {
            val history = AiSessionHistoryDo(role = it.role, content = it.content)
            history.inputToken = session.response.inputTokenCount ?: 0
            history.outputToken = session.response.outputTokenCount ?: 0
            history.totalToken = session.response.totalTokenCount ?: 0
            history
        }.forEach {
            d.addContent(it)
        }

        d.acePayloads = session.acePayload
    }

    private class OfflineSession(
        override val sessionId: String
    ) : ChatSession {
        override var response: ChatResponse
            get() = TODO("Not yet implemented")
            set(value) {}
        override val llm: LLM
            get() = TODO("Not yet implemented")
        override val history: MutableList<ChatMessage>
            get() = TODO()
        override val acePayload: MutableList<AceItem>
            get() = TODO()

        override fun workingMemory(prompt: String): List<ChatMessage> {
            TODO()
        }

        override fun generate(prompt: String): ChatSSE {
            TODO()
        }
    }
}