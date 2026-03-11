package com.dev.lib.agent.infra.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryMcpServerRegistryTest {

    private InMemoryMcpServerRegistry registry;

    @BeforeEach
    void setUp() {

        registry = new InMemoryMcpServerRegistry(Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void shouldRegisterNewServer() {

        McpServerDefinition registered = registry.register("dw", "Data Warehouse", "http://localhost:8081/sse", "desc");

        assertEquals("dw", registered.getServerId());
        assertEquals("Data Warehouse", registered.getName());
        assertEquals("http://localhost:8081/sse", registered.getUrl());
        assertEquals("REGISTERED", registered.getStatus());
    }

    @Test
    void shouldUpdateServerWhenRegisteringSameIdAgain() {

        registry.register("dw", "Data Warehouse", "http://localhost:8081/sse", "desc");
        McpServerDefinition updated = registry.register("dw", "DW", "http://localhost:8082/sse", "updated");

        assertEquals("DW", updated.getName());
        assertEquals("http://localhost:8082/sse", updated.getUrl());
        assertEquals("updated", updated.getDescription());
    }

    @Test
    void shouldListCurrentServers() {

        registry.register("dw", "DW", "http://localhost:8081/sse", "desc");
        registry.register("med", "Medical", "http://localhost:8082/sse", "desc");

        List<McpServerDefinition> servers = registry.list();

        assertEquals(2, servers.size());
        assertTrue(servers.stream().anyMatch(server -> "dw".equals(server.getServerId())));
        assertTrue(servers.stream().anyMatch(server -> "med".equals(server.getServerId())));
    }
}
