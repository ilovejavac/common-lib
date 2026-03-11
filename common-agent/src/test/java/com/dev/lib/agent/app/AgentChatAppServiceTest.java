package com.dev.lib.agent.app;

import com.dev.lib.agent.config.AgentProperties;
import com.dev.lib.agent.domain.model.AgentSession;
import com.dev.lib.agent.domain.service.SessionManager;
import com.dev.lib.agent.infra.agent.AgentExecutor;
import com.dev.lib.agent.infra.session.DefaultSessionManager;
import com.dev.lib.agent.infra.session.InMemorySessionRepository;
import com.dev.lib.agent.trigger.http.request.ChatRequest;
import com.dev.lib.agent.trigger.http.response.ChatAcceptedResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentChatAppServiceTest {

    @Test
    void shouldDrainPendingQueueAfterImmediateExecution() {

        AgentProperties properties = new AgentProperties();
        SessionManager sessionManager = new DefaultSessionManager(
                new InMemorySessionRepository(),
                Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ZoneOffset.UTC)
        );
        AtomicInteger invocationCount = new AtomicInteger();
        AgentExecutor executor = (session, message) -> invocationCount.incrementAndGet();
        AgentChatAppService appService = new AgentChatAppService(sessionManager, properties, executor);

        ChatRequest request = new ChatRequest();
        request.setPrompt("hello");

        ChatAcceptedResponse response = appService.accept(request);
        AgentSession session = sessionManager.find(response.getSessionId()).orElseThrow();

        assertEquals(1, invocationCount.get());
        assertTrue(session.getPendingQueue().isEmpty());
        assertEquals("IDLE", session.getStatus().name());
    }
}
