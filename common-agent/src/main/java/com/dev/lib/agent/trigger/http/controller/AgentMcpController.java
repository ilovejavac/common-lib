package com.dev.lib.agent.trigger.http.controller;

import com.dev.lib.agent.app.McpRegistryAppService;
import com.dev.lib.agent.infra.mcp.McpServerDefinition;
import com.dev.lib.agent.trigger.http.request.McpRegisterRequest;
import com.dev.lib.web.model.ServerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent/mcp")
public class AgentMcpController {

    private final McpRegistryAppService mcpRegistryAppService;

    @PostMapping("/register")
    public ServerResponse<McpServerDefinition> register(@Valid @RequestBody McpRegisterRequest request) {

        return ServerResponse.success(mcpRegistryAppService.register(request));
    }

    @GetMapping("/servers")
    public ServerResponse<List<McpServerDefinition>> list() {

        return ServerResponse.success(mcpRegistryAppService.list());
    }
}
