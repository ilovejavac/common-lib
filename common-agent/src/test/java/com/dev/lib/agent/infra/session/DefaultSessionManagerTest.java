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

        Clock clock = Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ZoneOffset.UTC);
        sessionManager = new DefaultSessionManager(new InMemorySessionRepository(), clock);
    }

    @Test
    void shouldCreateNewSessionWhenSessionIdMissing() {

        AgentSession session = sessionManager.getOrCreate(null);

        assertEquals(SessionStatus.IDLE, session.getStatus());
        assertTrue(sessionManager.find(session.getSessionId()).isPresent());
    }

    @Test
    void shouldReuseExistingSessionWhenSessionIdExists() {

        AgentSession created = sessionManager.getOrCreate("s-1");
        AgentSession reused = sessionManager.getOrCreate("s-1");

        assertSame(created, reused);
    }

    @Test
    void shouldOnlyMarkRunningOnceForSameSession() {

        AgentSession session = sessionManager.getOrCreate("s-1");

        assertTrue(sessionManager.markRunningIfIdle(session.getSessionId()));
        assertFalse(sessionManager.markRunningIfIdle(session.getSessionId()));
        assertEquals(SessionStatus.RUNNING, session.getStatus());
    }

    @Test
    void shouldDestroySession() {

        AgentSession session = sessionManager.getOrCreate("s-1");
        sessionManager.destroy(session.getSessionId());

        Optional<AgentSession> found = sessionManager.find("s-1");

        assertTrue(found.isEmpty());
    }
}
