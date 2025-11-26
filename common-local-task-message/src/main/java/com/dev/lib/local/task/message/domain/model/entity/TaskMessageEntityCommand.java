package com.dev.lib.local.task.message.domain.model.entity;

import lombok.Data;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.io.Serializable;

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

    private String taskId;
    private String taskName;
}
