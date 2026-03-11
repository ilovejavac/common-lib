package com.dev.lib.agent.infra.session;

import com.dev.lib.agent.domain.model.AgentSession;
import com.dev.lib.agent.domain.model.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSessionManagerTest {

    private DefaultSessionManager sessionManager;

    @BeforeEach
    void setUp() {

        // 固定时间让测试结果稳定，避免断言受当前系统时间影响。
        Clock clock = Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ZoneOffset.UTC);
        sessionManager = new DefaultSessionManager(new InMemorySessionRepository(), clock);
    }

    @Test
    // 验证未提供 sessionId 时会自动创建新会话并保存到仓库。
    void shouldCreateNewSessionWhenSessionIdMissing() {

        AgentSession session = sessionManager.getOrCreate(null);

        assertEquals(SessionStatus.IDLE, session.getStatus());
        assertTrue(sessionManager.find(session.getSessionId()).isPresent());
    }

    @Test
    // 验证同一个 sessionId 会复用已有会话，避免重复创建。
    void shouldReuseExistingSessionWhenSessionIdExists() {

        AgentSession created = sessionManager.getOrCreate("s-1");
        AgentSession reused = sessionManager.getOrCreate("s-1");

        assertSame(created, reused);
    }

    @Test
    // 验证同一会话只能从空闲态成功进入运行态一次，防止重复抢占执行权。
    void shouldOnlyMarkRunningOnceForSameSession() {

        AgentSession session = sessionManager.getOrCreate("s-1");

        assertTrue(sessionManager.markRunningIfIdle(session.getSessionId()));
        assertFalse(sessionManager.markRunningIfIdle(session.getSessionId()));
        assertEquals(SessionStatus.RUNNING, session.getStatus());
    }

    @Test
    // 验证销毁会话后仓库中不再能查到它，避免悬挂数据残留。
    void shouldDestroySession() {

        AgentSession session = sessionManager.getOrCreate("s-1");
        sessionManager.destroy(session.getSessionId());

        Optional<AgentSession> found = sessionManager.find("s-1");

        assertTrue(found.isEmpty());
    }
}
