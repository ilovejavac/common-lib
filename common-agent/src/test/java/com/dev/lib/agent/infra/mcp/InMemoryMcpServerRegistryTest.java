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

        // 固定注册时间，确保时间相关字段在测试中可预测。
        registry = new InMemoryMcpServerRegistry(Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    // 验证首次注册会写入完整的服务定义和默认状态。
    void shouldRegisterNewServer() {

        McpServerDefinition registered = registry.register("dw", "Data Warehouse", "http://localhost:8081/sse", "desc");

        assertEquals("dw", registered.getServerId());
        assertEquals("Data Warehouse", registered.getName());
        assertEquals("http://localhost:8081/sse", registered.getUrl());
        assertEquals("REGISTERED", registered.getStatus());
    }

    @Test
    // 验证相同 serverId 再次注册时会覆盖旧值，而不是产生重复记录。
    void shouldUpdateServerWhenRegisteringSameIdAgain() {

        registry.register("dw", "Data Warehouse", "http://localhost:8081/sse", "desc");
        McpServerDefinition updated = registry.register("dw", "DW", "http://localhost:8082/sse", "updated");

        assertEquals("DW", updated.getName());
        assertEquals("http://localhost:8082/sse", updated.getUrl());
        assertEquals("updated", updated.getDescription());
    }

    @Test
    // 验证列表查询能返回当前已注册的全部服务。
    void shouldListCurrentServers() {

        registry.register("dw", "DW", "http://localhost:8081/sse", "desc");
        registry.register("med", "Medical", "http://localhost:8082/sse", "desc");

        List<McpServerDefinition> servers = registry.list();

        assertEquals(2, servers.size());
        assertTrue(servers.stream().anyMatch(server -> "dw".equals(server.getServerId())));
        assertTrue(servers.stream().anyMatch(server -> "med".equals(server.getServerId())));
    }
}
