package com.dev.lib.agent.infra.stream;

import com.dev.lib.agent.domain.model.AgentMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SessionStreamHub {

    SseEmitter subscribe(String sessionId);

    void publish(String sessionId, AgentMessage message);

    void complete(String sessionId);
}
