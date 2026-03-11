package com.dev.lib.agent.app;

import com.dev.lib.agent.domain.service.SessionManager;
import com.dev.lib.agent.infra.stream.SessionStreamHub;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SessionStreamAppService {

    private final SessionManager sessionManager;
    private final SessionStreamHub sessionStreamHub;

    public SessionStreamAppService(SessionManager sessionManager, SessionStreamHub sessionStreamHub) {

        this.sessionManager = sessionManager;
        this.sessionStreamHub = sessionStreamHub;
    }

    public SseEmitter subscribe(String sessionId) {

        sessionManager.find(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
        return sessionStreamHub.subscribe(sessionId);
    }
}
