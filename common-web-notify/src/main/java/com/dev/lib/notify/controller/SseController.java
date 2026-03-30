package com.dev.lib.notify.controller;

import com.dev.lib.notify.core.SseEmitterManager;
import com.dev.lib.security.util.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 通知接口
 * 提供 SSE 连接注册端点
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SseController implements InitializingBean {

    private final SseEmitterManager emitterManager;

    @Override
    public void afterPropertiesSet() {

        emitterManager.init();
    }

    /**
     * 注册 SSE 连接
     * 前端调用此接口建立 SSE 连接
     *
     * @param clientId 可选，客户端 ID。如果不提供，自动生成
     * @return SseEmitter
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam(required = false) String clientId
    ) {

        if (clientId == null || clientId.isEmpty()) {
            Long userId = SecurityContextHolder.getUserId();
            clientId = userId != null ? String.valueOf(userId) : "";
        }
        log.info("New SSE subscription request from client: {}", clientId);
        return emitterManager.createEmitter(clientId);
    }

}
