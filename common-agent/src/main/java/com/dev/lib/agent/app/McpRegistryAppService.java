package com.dev.lib.agent.app;

import com.dev.lib.agent.infra.mcp.McpServerDefinition;
import com.dev.lib.agent.infra.mcp.McpServerRegistry;
import com.dev.lib.agent.trigger.http.request.McpRegisterRequest;

import java.util.List;

public class McpRegistryAppService {

    private final McpServerRegistry mcpServerRegistry;

    public McpRegistryAppService(McpServerRegistry mcpServerRegistry) {

        this.mcpServerRegistry = mcpServerRegistry;
    }

    public McpServerDefinition register(McpRegisterRequest request) {

        return mcpServerRegistry.register(
                request.getServerId(),
                request.getName(),
                request.getUrl(),
                request.getDescription()
        );
    }

    public List<McpServerDefinition> list() {

        return mcpServerRegistry.list();
    }
}
