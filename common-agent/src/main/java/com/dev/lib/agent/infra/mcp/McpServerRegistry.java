package com.dev.lib.agent.infra.mcp;

import java.util.List;

public interface McpServerRegistry {

    McpServerDefinition register(String serverId, String name, String url, String description);

    List<McpServerDefinition> list();
}
