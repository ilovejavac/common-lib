package com.dev.lib.notify;

import com.dev.lib.notify.context.ClientIdHolder;
import com.dev.lib.notify.core.SseEmitterManager;
import com.dev.lib.notify.model.Message;
import com.dev.lib.security.util.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * WebNotify 静态入口类
 * 提供链式 API 发送 SSE 消息
 * <p>
 * 使用示例:
 * <pre>
 * // 指定客户端 ID 发送
 * WebNotify.topic("user.update")
 *     .message(new UserUpdateMessage(user))
 *     .send("client-123");
 *
 * // 自动获取当前请求的客户端 ID
 * WebNotify.topic("notification")
 *     .message(new NotifyMessage("Hello"))
 *     .send();
 * </pre>
 */
@Slf4j
@Component
public class WebNotify implements InitializingBean {

    private static WebNotify instance;

    private final SseEmitterManager emitterManager;

    public WebNotify(SseEmitterManager emitterManager) {

        this.emitterManager = emitterManager;
    }

    @Override
    public void afterPropertiesSet() {

        instance = this;
    }

    /**
     * 创建主题构建器
     *
     * @param topic 主题名称（即 SSE 事件名称）
     * @return TopicBuilder
     */
    public static TopicBuilder topic(String topic) {

        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException("topic must not be empty");
        }
        return new TopicBuilder(topic);
    }

    /**
     * 主题构建器
     */
    public static class TopicBuilder {

        private final String topic;

        TopicBuilder(String topic) {

            this.topic = topic;
        }

        /**
         * 设置消息
         *
         * @param message 消息对象
         * @return MessageBuilder
         */
        public MessageBuilder message(Message message) {

            if (message == null) {
                throw new IllegalArgumentException("message must not be null");
            }
            return new MessageBuilder(topic, message);
        }

    }

    /**
     * 消息构建器
     */
    public static class MessageBuilder {

        private final String  topic;  // topic = 事件名称

        private final Message message;

        MessageBuilder(String topic, Message message) {

            this.topic = topic;
            this.message = message;
        }

        /**
         * 发送消息到当前请求的客户端
         * 自动从 ThreadLocal 获取客户端 ID
         *
         * @return 是否发送成功
         */
        public boolean send() {

            String clientId = ClientIdHolder.getClientId();
            if (clientId == null) {
                clientId = SecurityContextHolder.getUserId() + "";
            }
            return send(clientId);
        }

        /**
         * 发送消息到指定客户端
         *
         * @param clientId 客户端 ID
         * @return 是否发送成功
         */
        public boolean send(String clientId) {

            if (clientId == null || clientId.isEmpty()) {
                log.warn("Client ID is empty, message not sent. Topic: {}", topic);
                return false;
            }
            if (instance == null) {
                log.error("WebNotify instance not initialized");
                return false;
            }
            // 传递 topic 作为事件名称
            boolean success = instance.emitterManager.sendMessage(clientId, topic, message);
            if (!success) {
                log.warn("Failed to send message. Topic: {}, Client: {}", topic, clientId);
            }
            return success;
        }

    }

}
