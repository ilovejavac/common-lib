package com.dev.lib.agent.domain.service;

import com.dev.lib.agent.domain.model.AgentSession;

import java.util.Optional;

public interface SessionManager {

    AgentSession getOrCreate(String sessionId);

    Optional<AgentSession> find(String sessionId);

    boolean markRunningIfIdle(String sessionId);

    void markIdle(String sessionId);

    void destroy(String sessionId);
}
