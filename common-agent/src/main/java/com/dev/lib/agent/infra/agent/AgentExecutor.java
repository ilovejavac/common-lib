package com.dev.lib.agent.infra.agent;

import com.dev.lib.agent.domain.model.AgentSession;
import com.dev.lib.agent.domain.model.PendingUserMessage;

public interface AgentExecutor {

    void execute(AgentSession session, PendingUserMessage message);
}
