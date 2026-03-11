package com.dev.lib.agent.trigger.http.controller;

import com.dev.lib.agent.app.AgentChatAppService;
import com.dev.lib.agent.trigger.http.request.ChatRequest;
import com.dev.lib.agent.trigger.http.response.ChatAcceptedResponse;
import com.dev.lib.web.model.ServerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent")
public class AgentChatController {

    private final AgentChatAppService agentChatAppService;

    @PostMapping("/chat")
    public ServerResponse<ChatAcceptedResponse> chat(@Valid @RequestBody ChatRequest request) {

        return ServerResponse.success(agentChatAppService.accept(request));
    }
}
