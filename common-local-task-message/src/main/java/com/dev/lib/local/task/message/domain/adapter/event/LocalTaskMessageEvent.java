package com.dev.lib.local.task.message.domain.adapter.event;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;

public interface LocalTaskMessageEvent {

    void publish(TaskMessageEntityCommand cmd);
}
