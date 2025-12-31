package com.dev.lib.ai.service.agent.tool

import com.dev.lib.ai.service.agent.Tool
import org.springframework.context.annotation.Configuration

@Configuration
class AgentToolRegistry(
    val tools: List<Tool>
) {
}