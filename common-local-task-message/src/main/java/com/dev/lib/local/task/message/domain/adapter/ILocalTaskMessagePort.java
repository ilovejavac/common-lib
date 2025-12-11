package com.dev.lib.local.task.message.domain.adapter;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;

public interface ILocalTaskMessagePort {

    String notify2http(TaskMessageEntityCommand cmd);

    String notify2rabbit(TaskMessageEntityCommand cmd);

}
