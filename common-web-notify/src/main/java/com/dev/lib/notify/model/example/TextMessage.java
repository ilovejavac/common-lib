package com.dev.lib.notify.model.example;

import com.dev.lib.notify.model.Message;
import lombok.Getter;

/**
 * 文本消息示例
 * <p>
 * 使用示例:
 * <pre>
 * TextMessage message = new TextMessage("Hello, World!");
 * WebNotify.topic("notification").message(message).send(clientId);
 * </pre>
 */
@Getter
public class TextMessage extends Message {

    private final String content;

    public TextMessage(String content) {
        this.content = content;
    }

    @Override
    public Object getData() {
        return new Data(content);
    }

    @Getter
    private static class Data {
        private final String content;

        Data(String content) {
            this.content = content;
        }
    }
}
