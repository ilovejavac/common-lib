package com.dev.lib.notify.controller;

import com.dev.lib.notify.core.SseEmitterManager;
import com.dev.lib.notify.model.Message;
import com.dev.lib.notify.model.example.TextMessage;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
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

    /**
     * 生成客户端 ID
     * 前端可以先调用此接口获取 clientId，再建立 SSE 连接
     *
     * @return 客户端 ID
     */
    @GetMapping("/client-id")
    public ServerResponse<String> generateClientId() {

        Long userId = SecurityContextHolder.getUserId();
        String clientId = userId != null ? String.valueOf(userId) : "";
        return ServerResponse.success(clientId);
    }

    /**
     * 测试消息发送
     * 仅用于开发测试
     *
     * @param clientId 客户端 ID
     * @return 发送结果
     */
    @PostMapping("/test-send")
    public ServerResponse<Boolean> testSend(@RequestParam String clientId) {

        // 使用示例消息类测试，传递 topic "test"
        var message = new TextMessage("Test message from server");
        boolean sent = emitterManager.sendMessage(clientId, "test", message);
        return ServerResponse.success(sent);
    }

    /**
     * 获取当前连接数
     *
     * @return 连接数
     */
    @GetMapping("/connections")
    public ServerResponse<Integer> getConnectionCount() {

        return ServerResponse.success(emitterManager.getConnectionCount());
    }

    /**
     * 检查客户端是否在线
     *
     * @param clientId 客户端 ID
     * @return 是否在线
     */
    @GetMapping("/online")
    public ServerResponse<Boolean> isOnline(@RequestParam String clientId) {

        return ServerResponse.success(emitterManager.isOnline(clientId));
    }

}
