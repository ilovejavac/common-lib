package com.dev.lib.local.task.message.domain.model.entity;

import com.dev.lib.local.task.message.domain.model.NotifyType;
import lombok.Data;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.io.Serializable;
import java.util.Map;

@Data
public class TaskMessageEntityCommand implements Serializable {
    @Getter
    public static class Event extends ApplicationEvent {
        private final TaskMessageEntityCommand cmd;

        public Event(Object source, TaskMessageEntityCommand cmd) {
            super(source);
            this.cmd = cmd;
        }
    }

    @Data
    public static class NotifyConfig {
        @Data
        public static class Rabbit {
            private String topic;
            private String exchange;
        }

        @Data
        public static class Http {
            private String url;
            private String method;
            private String contentType;
            private String authorization;
        }

        private Rabbit rabbit;
        private Http http;
    }

    private String taskId;
    private String taskName;

    private NotifyType notifyType;

    private NotifyConfig notifyConfig;

    private String taskType;          // 任务类型（区分处理器）

    private String businessId;        // 业务ID（幂等 key）

    private Map<String, Object> payload;  // 任务参数

    private int maxRetry = 3;             // 最大重试次数
}
