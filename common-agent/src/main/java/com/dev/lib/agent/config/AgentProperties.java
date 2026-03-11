package com.dev.lib.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.agent")
public class AgentProperties {

    private boolean enabled = true;

    private int sessionTimeoutMinutes = 30;

    private int maxHistoryMessages = 50;

    private int maxPendingMessages = 100;

    private long sseTimeoutMs = 300_000L;

    private int maxPromptLength = 10_000;
}
