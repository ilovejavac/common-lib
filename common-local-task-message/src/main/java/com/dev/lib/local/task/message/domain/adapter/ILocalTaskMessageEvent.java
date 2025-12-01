package com.dev.lib.local.task.message.domain.adapter;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;

public interface ILocalTaskMessageEvent {

    void saveMessage(TaskMessageEntityCommand cmd);

    void publish(TaskMessageEntityCommand cmd);
}
