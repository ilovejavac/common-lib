package com.dev.lib.agent.infra.session;

import com.dev.lib.agent.domain.model.AgentSession;
import com.dev.lib.agent.domain.model.SessionStatus;
import com.dev.lib.agent.domain.service.SessionManager;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class DefaultSessionManager implements SessionManager {

    private final InMemorySessionRepository repository;
    private final Clock clock;

    public DefaultSessionManager(InMemorySessionRepository repository, Clock clock) {

        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public AgentSession getOrCreate(String sessionId) {

        if (sessionId != null && !sessionId.isBlank()) {
            return repository.find(sessionId)
                    .orElseGet(() -> createSession(sessionId));
        }
        return createSession(UUID.randomUUID().toString());
    }

    @Override
    public Optional<AgentSession> find(String sessionId) {

        return repository.find(sessionId);
    }

    @Override
    public boolean markRunningIfIdle(String sessionId) {

        AgentSession session = repository.find(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));

        synchronized (session) {
            if (session.getStatus() != SessionStatus.IDLE) {
                return false;
            }
            session.markRunning(now());
            return true;
        }
    }

    @Override
    public void markIdle(String sessionId) {

        repository.find(sessionId).ifPresent(session -> {
            synchronized (session) {
                if (!session.isClosed()) {
                    session.markIdle(now());
                }
            }
        });
    }

    @Override
    public void destroy(String sessionId) {

        repository.find(sessionId).ifPresent(AgentSession::close);
        repository.remove(sessionId);
    }

    private AgentSession createSession(String sessionId) {

        AgentSession session = AgentSession.create(sessionId, now());
        repository.save(session);
        return session;
    }

    private Instant now() {

        return Instant.now(clock);
    }
}
