package com.dev.lib.agent.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSessionTest {

    @Test
    // 验证新建会话的默认状态，确保后续状态流转有可信起点。
    void shouldStartIdleWhenCreated() {

        AgentSession session = AgentSession.create("s-1", Instant.parse("2026-03-11T00:00:00Z"));

        assertEquals(SessionStatus.IDLE, session.getStatus());
        assertTrue(session.getHistory().isEmpty());
        assertTrue(session.getPendingQueue().isEmpty());
    }

    @Test
    // 验证历史超限时只淘汰最旧的非系统消息，系统消息必须保留。
    void shouldTrimOldestNonSystemMessagesWhenHistoryExceedsLimit() {

        AgentSession session = AgentSession.create("s-1", Instant.parse("2026-03-11T00:00:00Z"));

        session.appendHistory(AgentMessage.of("m-1", "s-1", MessageRole.SYSTEM, MessageType.TEXT, "system", Instant.now()), 2);
        session.appendHistory(AgentMessage.of("m-2", "s-1", MessageRole.USER, MessageType.TEXT, "u1", Instant.now()), 2);
        session.appendHistory(AgentMessage.of("m-3", "s-1", MessageRole.ASSISTANT, MessageType.TEXT, "a1", Instant.now()), 2);
        session.appendHistory(AgentMessage.of("m-4", "s-1", MessageRole.USER, MessageType.TEXT, "u2", Instant.now()), 2);

        assertEquals(3, session.getHistory().size());
        assertEquals("m-1", session.getHistory().getFirst().getMessageId());
        assertEquals("m-3", session.getHistory().get(1).getMessageId());
        assertEquals("m-4", session.getHistory().get(2).getMessageId());
    }

    @Test
    // 验证待处理队列达到上限后拒绝继续入队，避免内存和调度失控。
    void shouldRejectPendingMessageWhenQueueIsFull() {

        AgentSession session = AgentSession.create("s-1", Instant.parse("2026-03-11T00:00:00Z"));
        session.enqueue(PendingUserMessage.of("m-1", "hello", Instant.now()), 1);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> session.enqueue(PendingUserMessage.of("m-2", "world", Instant.now()), 1)
        );

        assertTrue(error.getMessage().contains("pending"));
    }

    @Test
    // 验证关闭后的会话不再接受新消息，避免终态对象被再次写入。
    void shouldRejectNewMessageWhenSessionClosed() {

        AgentSession session = AgentSession.create("s-1", Instant.parse("2026-03-11T00:00:00Z"));
        session.close();

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> session.enqueue(PendingUserMessage.of("m-1", "hello", Instant.now()), 1)
        );

        assertTrue(error.getMessage().contains("closed"));
    }
}
