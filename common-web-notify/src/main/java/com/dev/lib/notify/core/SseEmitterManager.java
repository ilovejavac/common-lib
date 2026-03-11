package com.dev.lib.notify.core;

import com.dev.lib.notify.config.SseProperties;
import com.dev.lib.notify.model.Message;
import com.dev.lib.util.Jsons;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SSE Emitter 管理器
 * 管理所有客户端的 SSE 连接
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterManager {

    private final SseProperties sseProperties;

    /**
     * 存储 clientId -> SseEmitter 的映射
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 心跳线程池
     */
    private final ScheduledThreadPoolExecutor heartbeatExecutor = new ScheduledThreadPoolExecutor(1);

    /**
     * 初始化心跳任务
     */
    public void init() {

        if (!sseProperties.getEnabled()) {
            log.info("SSE is disabled by configuration");
            return;
        }

        heartbeatExecutor.scheduleAtFixedRate(
                this::sendHeartbeat,
                sseProperties.getHeartbeatInterval(),
                sseProperties.getHeartbeatInterval(),
                TimeUnit.SECONDS
        );
        log.info(
                "SSE initialized with non-expiring connections, heartbeat: {}s",
                sseProperties.getHeartbeatInterval()
        );
    }

    /**
     * 创建新的 SSE 连接
     *
     * @param clientId 客户端 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(String clientId) {
        // 检查连接数限制
        if (sseProperties.getMaxConnections() > 0
                && emitters.size() >= sseProperties.getMaxConnections()) {
            log.warn("Max connections limit reached: {}", sseProperties.getMaxConnections());
            throw new IllegalStateException("Maximum SSE connections reached");
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(clientId, emitter);

        // 设置超时和完成回调
        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for client: {}", clientId);
            removeEmitter(clientId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timeout for client: {}", clientId);
            removeEmitter(clientId);
        });

        emitter.onError((e) -> {
            log.error("SSE connection error for client: {}", clientId, e);
            removeEmitter(clientId);
        });

        log.info(
                "SSE connection created for client: {}, non-expiring",
                clientId
        );

        sendConnected(clientId);
        return emitter;
    }

    /**
     * 移除 SSE 连接
     *
     * @param clientId 客户端 ID
     */
    public void removeEmitter(String clientId) {

        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            emitter.complete();
        }
        log.info("SSE connection removed for client: {}", clientId);
    }

    /**
     * 发送消息给指定客户端
     *
     * @param clientId 客户端 ID
     * @param topic    事件名称（topic）
     * @param message  消息
     * @return 是否发送成功
     */
    public boolean sendMessage(String clientId, String topic, Message message) {

        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            log.warn("No SSE connection found for client: {}", clientId);
            return false;
        }

        try {
            // 使用 topic 作为事件名称，message 只提供数据
            String data = Jsons.toJson(message.getData());
            emitter.send(SseEmitter.event()
                                 .name(topic)
                                 .data(data)
                                 .id(message.getMessageId())
            );
            log.debug("Message sent to client: {}, event: {}, data: {}", clientId, topic, data);
            return true;
        } catch (IOException e) {
            log.error("Failed to send message to client: {}", clientId, e);
            removeEmitter(clientId);
            return false;
        }
    }

    /**
     * 广播消息给所有客户端
     *
     * @param topic   事件名称（topic）
     * @param message 消息
     * @return 成功发送的客户端数量
     */
    public int broadcast(String topic, Message message) {

        int successCount = 0;
        for (String clientId : emitters.keySet()) {
            if (sendMessage(clientId, topic, message)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 发送心跳
     * <p>
     * 作用：检测连接是否存活，防止中间代理/防火墙因"空闲"断开连接
     * 注意：无法延长 SSE 的超时时间
     */
    private void sendHeartbeat() {

        for (String clientId : emitters.keySet()) {
            SseEmitter emitter = emitters.get(clientId);
            if (emitter != null) {
                try {
                    // 使用 comment() 而非 data()，避免客户端误处理
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                    log.trace("Heartbeat sent to client: {}", clientId);
                } catch (IOException e) {
                    log.warn("Failed to send heartbeat to client: {}, removing", clientId, e);
                    removeEmitter(clientId);
                }
            }
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {

        return emitters.size();
    }

    /**
     * 检查客户端是否在线
     */
    public boolean isOnline(String clientId) {

        return emitters.containsKey(clientId);
    }

    /**
     * 发送连接成功消息
     *
     * @param clientId 客户端 ID
     */
    public void sendConnected(String clientId) {

        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                                     .name("_connected")
                                     .data("{\"clientId\":\"" + clientId + "\",\"timestamp\":" + System.currentTimeMillis() + "}")
                );
                log.debug("Connected message sent to client: {}", clientId);
            } catch (IOException e) {
                log.warn("Failed to send connected message to client: {}", clientId, e);
            }
        }
    }

    /**
     * 获取原始 SseEmitter（用于流式传输等高级场景）
     *
     * @param clientId 客户端 ID
     * @return SseEmitter
     */
    public SseEmitter getEmitter(String clientId) {

        return emitters.get(clientId);
    }

}
