package com.dev.lib.agent.infra.agent;

import com.dev.lib.agent.domain.model.AgentSession;
import com.dev.lib.agent.domain.model.PendingUserMessage;

public class NoOpAgentExecutor implements AgentExecutor {

    @Override
    public void execute(AgentSession session, PendingUserMessage message) {
        // The first phase only builds the orchestration skeleton.
    }
}
