package com.dev.lib.agent.trigger.http.controller;

import com.dev.lib.agent.app.SessionQueryAppService;
import com.dev.lib.agent.trigger.http.response.SessionDetailResponse;
import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent/session")
public class AgentSessionController {

    private final SessionQueryAppService sessionQueryAppService;

    @GetMapping("/{sessionId}/history")
    public ServerResponse<SessionDetailResponse> history(@PathVariable String sessionId) {

        return ServerResponse.success(SessionDetailResponse.from(sessionQueryAppService.get(sessionId)));
    }

    @DeleteMapping("/{sessionId}")
    public ServerResponse<Boolean> destroy(@PathVariable String sessionId) {

        sessionQueryAppService.destroy(sessionId);
        return ServerResponse.success(Boolean.TRUE);
    }
}
