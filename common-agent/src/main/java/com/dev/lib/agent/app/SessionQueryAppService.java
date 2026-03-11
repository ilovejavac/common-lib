package com.dev.lib.agent.app;

import com.dev.lib.agent.domain.model.AgentSession;
import com.dev.lib.agent.domain.service.SessionManager;

public class SessionQueryAppService {

    private final SessionManager sessionManager;

    public SessionQueryAppService(SessionManager sessionManager) {

        this.sessionManager = sessionManager;
    }

    public AgentSession get(String sessionId) {

        return sessionManager.find(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
    }

    public void destroy(String sessionId) {

        sessionManager.destroy(sessionId);
    }
}
