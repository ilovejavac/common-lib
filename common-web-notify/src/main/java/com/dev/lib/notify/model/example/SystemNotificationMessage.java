package com.dev.lib.notify.model.example;

import com.dev.lib.notify.model.Message;
import lombok.Getter;

/**
 * 系统通知消息示例
 * <p>
 * 使用示例:
 * <pre>
 * SystemNotificationMessage message = new SystemNotificationMessage("系统提示", "您有新的订单", "info");
 * WebNotify.topic("system.notification").message(message).send(clientId);
 * </pre>
 */
@Getter
public class SystemNotificationMessage extends Message {

    private final String title;
    private final String content;
    private final String level;

    public SystemNotificationMessage(String title, String content, String level) {
        this.title = title;
        this.content = content;
        this.level = level;
    }

    @Override
    public Object getData() {
        return new NotificationData(title, content, level);
    }

    @Getter
    private static class NotificationData {
        private final String title;
        private final String content;
        private final String level;

        NotificationData(String title, String content, String level) {
            this.title = title;
            this.content = content;
            this.level = level;
        }
    }
}
