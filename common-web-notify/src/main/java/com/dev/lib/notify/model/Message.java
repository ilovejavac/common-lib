package com.dev.lib.notify.model;

import com.dev.lib.entity.id.IDWorker;
import lombok.Getter;

/**
 * SSE 消息基类
 * 所有发送给前端的消息都需要继承此类
 * <p>
 * 注意：消息本身不包含类型，类型由发送时的 topic 决定
 */
@Getter
public abstract class Message {

    /**
     * 消息唯一 ID
     */
    private final String messageId;

    /**
     * 消息时间戳
     */
    private final long timestamp;

    protected Message() {
        this.messageId = IDWorker.newId();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取消息数据，由子类实现
     */
    public abstract Object getData();
}
