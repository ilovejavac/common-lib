package com.dev.lib.ai.data.adapt

import com.dev.lib.ai.data.dao.AiSessionDao
import com.dev.lib.ai.data.dao.AiSessionHistoryDao
import com.dev.lib.ai.data.entity.AiSessionDo
import com.dev.lib.ai.data.entity.AiSessionHistoryDo
import com.dev.lib.ai.model.AceItem
import com.dev.lib.ai.model.ChatItem
import com.dev.lib.ai.model.ChatResponse
import com.dev.lib.ai.model.ChatSSE
import com.dev.lib.ai.repo.AiSessionStore
import com.dev.lib.ai.service.agent.AiAgent
import com.dev.lib.ai.service.agent.AiChatSession
import com.dev.lib.ai.service.agent.ChatSession
import com.dev.lib.ai.service.agent.support.skill.SkillToolManager
import com.dev.lib.ai.service.llm.LLM
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SessionStorage(
    val dao: AiSessionDao,
    val history: AiSessionHistoryDao
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
        val session = dao.load(AiSessionDao.Q().apply {
            bizId = id
        }).orElseThrow { IllegalArgumentException("会话不存在: $id") }

        val model = llm
            ?: session.model?.toLLM(session.tokenLimit)
            ?: throw IllegalStateException("未关联有效LLM模型")

        return AiChatSession(
            sessionId = id,
            llm = model,
            agent = AiAgent(),
            history = history.loads(AiSessionHistoryDao.Q().apply {
                limit = 20
                sessionId = session.id!!
            }).reversed().toChatMessage(),
            acePayload = session.acePayloads
        )
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun storeSessionHistory(session: ChatSession) {
        val sessionDo = dao.load(AiSessionDao.Q().apply {
            bizId = session.sessionId
        }).orElseThrow()
        val last2 = session.history.takeLast(2)

        history.saveAll(last2.map {
            AiSessionHistoryDo(content = it.content, role = it.role).apply {
                tokenUsage = it.token
            }
        }.map {
            sessionDo.setContent(it)
        })
    }

    private fun List<AiSessionHistoryDo>.toChatMessage() =
        map(AiSessionHistoryDo::toChatMessage)
            .toMutableList()

    private class OfflineSession(
        override val sessionId: String
    ) : ChatSession {
        override var skillToolManager: SkillToolManager
            get() = TODO("Not yet implemented")
            set(value) {}
        override var response: ChatResponse
            get() = TODO("Not yet implemented")
            set(value) {}
        override val llm: LLM
            get() = TODO("Not yet implemented")
        override val history: MutableList<ChatItem>
            get() = TODO()
        override val acePayload: MutableList<AceItem>
            get() = TODO()

        override fun workingMemory(prompt: String): List<ChatItem> {
            TODO()
        }

        override fun generate(prompt: String, block: () -> Unit): ChatSSE {
            TODO("Not yet implemented")
        }
    }
}