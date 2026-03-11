package com.dev.lib.agent.infra.session;

import com.dev.lib.agent.domain.model.AgentSession;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionRepository {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    public AgentSession save(AgentSession session) {

        sessions.put(session.getSessionId(), session);
        return session;
    }

    public Optional<AgentSession> find(String sessionId) {

        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void remove(String sessionId) {

        sessions.remove(sessionId);
    }

    public Collection<AgentSession> findAll() {

        return sessions.values();
    }
}
