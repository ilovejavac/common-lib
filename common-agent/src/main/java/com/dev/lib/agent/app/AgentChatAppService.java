package com.dev.lib.agent.app;

import com.dev.lib.agent.config.AgentProperties;
import com.dev.lib.agent.domain.model.AgentMessage;
import com.dev.lib.agent.domain.model.AgentSession;
import com.dev.lib.agent.domain.model.MessageRole;
import com.dev.lib.agent.domain.model.MessageType;
import com.dev.lib.agent.domain.model.PendingUserMessage;
import com.dev.lib.agent.domain.service.SessionManager;
import com.dev.lib.agent.infra.agent.AgentExecutor;
import com.dev.lib.agent.trigger.http.request.ChatRequest;
import com.dev.lib.agent.trigger.http.response.ChatAcceptedResponse;

import java.time.Instant;
import java.util.UUID;

public class AgentChatAppService {

    private final SessionManager sessionManager;
    private final AgentProperties properties;
    private final AgentExecutor agentExecutor;

    public AgentChatAppService(
            SessionManager sessionManager,
            AgentProperties properties,
            AgentExecutor agentExecutor
    ) {

        this.sessionManager = sessionManager;
        this.properties = properties;
        this.agentExecutor = agentExecutor;
    }

    public ChatAcceptedResponse accept(ChatRequest request) {

        AgentSession session = sessionManager.getOrCreate(request.getSessionId());
        Instant now = Instant.now();
        String messageId = UUID.randomUUID().toString();

        session.appendHistory(
                AgentMessage.of(
                        messageId,
                        session.getSessionId(),
                        MessageRole.USER,
                        MessageType.TEXT,
                        request.getPrompt(),
                        now
                ),
                properties.getMaxHistoryMessages()
        );

        PendingUserMessage pendingUserMessage = PendingUserMessage.of(messageId, request.getPrompt(), now);
        session.enqueue(pendingUserMessage, properties.getMaxPendingMessages());

        if (sessionManager.markRunningIfIdle(session.getSessionId())) {
            PendingUserMessage nextMessage = session.pollPending();
            try {
                if (nextMessage != null) {
                    agentExecutor.execute(session, nextMessage);
                }
            } finally {
                sessionManager.markIdle(session.getSessionId());
            }
        }

        return new ChatAcceptedResponse(session.getSessionId(), messageId, true);
    }
}
