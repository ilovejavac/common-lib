package com.dev.lib.local.task.message.domain.adapter.event;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;

public interface LocalTaskMessageEvent {

    void saveMessage(TaskMessageEntityCommand cmd);

    void publish(TaskMessageEntityCommand cmd);
}
