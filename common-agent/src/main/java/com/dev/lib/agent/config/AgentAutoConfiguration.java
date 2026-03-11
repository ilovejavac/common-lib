package com.dev.lib.agent.config;

import com.dev.lib.agent.app.AgentChatAppService;
import com.dev.lib.agent.app.McpRegistryAppService;
import com.dev.lib.agent.app.SessionQueryAppService;
import com.dev.lib.agent.app.SessionStreamAppService;
import com.dev.lib.agent.domain.service.SessionManager;
import com.dev.lib.agent.infra.agent.AgentExecutor;
import com.dev.lib.agent.infra.agent.NoOpAgentExecutor;
import com.dev.lib.agent.infra.mcp.InMemoryMcpServerRegistry;
import com.dev.lib.agent.infra.mcp.McpServerRegistry;
import com.dev.lib.agent.infra.session.DefaultSessionManager;
import com.dev.lib.agent.infra.session.InMemorySessionRepository;
import com.dev.lib.agent.infra.stream.SessionStreamHub;
import com.dev.lib.agent.infra.stream.SseSessionStreamHub;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@AutoConfiguration
@EnableConfigurationProperties(AgentProperties.class)
@ConditionalOnProperty(prefix = "app.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Clock agentClock() {

        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public InMemorySessionRepository inMemorySessionRepository() {

        return new InMemorySessionRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionManager sessionManager(InMemorySessionRepository repository, Clock agentClock) {

        return new DefaultSessionManager(repository, agentClock);
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionStreamHub sessionStreamHub(AgentProperties properties) {

        return new SseSessionStreamHub(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpServerRegistry mcpServerRegistry(Clock agentClock) {

        return new InMemoryMcpServerRegistry(agentClock);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentExecutor agentExecutor() {

        return new NoOpAgentExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentChatAppService agentChatAppService(
            SessionManager sessionManager,
            AgentProperties properties,
            AgentExecutor agentExecutor
    ) {

        return new AgentChatAppService(sessionManager, properties, agentExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionQueryAppService sessionQueryAppService(SessionManager sessionManager) {

        return new SessionQueryAppService(sessionManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public SessionStreamAppService sessionStreamAppService(
            SessionManager sessionManager,
            SessionStreamHub sessionStreamHub
    ) {

        return new SessionStreamAppService(sessionManager, sessionStreamHub);
    }

    @Bean
    @ConditionalOnMissingBean
    public McpRegistryAppService mcpRegistryAppService(McpServerRegistry mcpServerRegistry) {

        return new McpRegistryAppService(mcpServerRegistry);
    }
}
