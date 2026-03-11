package com.dev.lib.agent.trigger.http.response;

import com.dev.lib.agent.domain.model.AgentMessage;
import com.dev.lib.agent.domain.model.AgentSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SessionDetailResponse {

    private final String sessionId;
    private final String status;
    private final List<AgentMessage> messages;

    public static SessionDetailResponse from(AgentSession session) {

        return new SessionDetailResponse(
                session.getSessionId(),
                session.getStatus().name(),
                session.getHistory()
        );
    }
}
