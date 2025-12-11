package com.dev.lib.local.task.message.domain.service;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;

public interface ITaskNotifyService {

    String notify(TaskMessageEntityCommand command);

}
