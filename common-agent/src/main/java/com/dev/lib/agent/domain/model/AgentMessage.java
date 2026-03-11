package com.dev.lib.agent.domain.model;

import java.time.Instant;

public record AgentMessage(
        String messageId,
        String sessionId,
        MessageRole role,
        MessageType type,
        String content,
        Instant timestamp
) {

    public static AgentMessage of(
            String messageId,
            String sessionId,
            MessageRole role,
            MessageType type,
            String content,
            Instant timestamp
    ) {

        return new AgentMessage(messageId, sessionId, role, type, content, timestamp);
    }

    public String getMessageId() {

        return messageId;
    }

    public String getSessionId() {

        return sessionId;
    }

    public MessageRole getRole() {

        return role;
    }

    public MessageType getType() {

        return type;
    }

    public String getContent() {

        return content;
    }

    public Instant getTimestamp() {

        return timestamp;
    }
}
