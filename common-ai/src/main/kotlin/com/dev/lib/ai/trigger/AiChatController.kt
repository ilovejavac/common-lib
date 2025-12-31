package com.dev.lib.ai.trigger

import com.dev.lib.ai.service.AiService
import com.dev.lib.ai.trigger.dto.AgentChatResponse
import com.dev.lib.ai.trigger.request.AgentChatRequest
import com.dev.lib.web.model.ServerResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * 对话接口
 *
 * 负责会话创建，实际对话
 */
@RestController
class AiChatController(
    val service: AiService
) {

    /**
     * 获取用户所有会话
     */
    @GetMapping("/api/agent/chat/sessions")
    fun listSessions(): ServerResponse<List<AgentChatResponse.SessionDto>> =
        ServerResponse.success(listOf())

    /**
     * 新增会话
     */
    @PostMapping("/api/agent/chat/open-session")
    fun openSession(): ServerResponse<String> =
        ServerResponse.success(service.opensession())

    @PostMapping("/api/agent/chat")
    fun chat(@Validated @RequestBody cmd: AgentChatRequest.Chat): SseEmitter =
        service.chat(cmd)
}