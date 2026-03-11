package com.dev.lib.agent.infra.mcp;

import java.time.Instant;

public class McpServerDefinition {

    private final String serverId;
    private final String name;
    private final String url;
    private final String description;
    private final String status;
    private final Instant registeredAt;

    public McpServerDefinition(
            String serverId,
            String name,
            String url,
            String description,
            String status,
            Instant registeredAt
    ) {

        this.serverId = serverId;
        this.name = name;
        this.url = url;
        this.description = description;
        this.status = status;
        this.registeredAt = registeredAt;
    }

    public String getServerId() {

        return serverId;
    }

    public String getName() {

        return name;
    }

    public String getUrl() {

        return url;
    }

    public String getDescription() {

        return description;
    }

    public String getStatus() {

        return status;
    }

    public Instant getRegisteredAt() {

        return registeredAt;
    }
}
