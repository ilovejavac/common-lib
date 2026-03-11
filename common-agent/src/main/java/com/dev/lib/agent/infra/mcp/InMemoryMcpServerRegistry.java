package com.dev.lib.agent.infra.mcp;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMcpServerRegistry implements McpServerRegistry {

    private final Map<String, McpServerDefinition> servers = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryMcpServerRegistry(Clock clock) {

        this.clock = clock;
    }

    @Override
    public McpServerDefinition register(String serverId, String name, String url, String description) {

        McpServerDefinition definition = new McpServerDefinition(
                serverId,
                name,
                url,
                description,
                "REGISTERED",
                Instant.now(clock)
        );
        servers.put(serverId, definition);
        return definition;
    }

    @Override
    public List<McpServerDefinition> list() {

        return servers.values().stream()
                .sorted(Comparator.comparing(McpServerDefinition::getServerId))
                .toList();
    }
}
