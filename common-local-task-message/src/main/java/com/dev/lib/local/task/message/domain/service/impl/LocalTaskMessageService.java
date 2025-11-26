package com.dev.lib.local.task.message.domain.service.impl;

import com.dev.lib.local.task.message.domain.adapter.event.LocalTaskMessageEvent;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.service.LocalTaskMessageHandleService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTaskMessageService implements LocalTaskMessageHandleService {

    @Resource
    private LocalTaskMessageEvent event;

    @Override
    public void handle(TaskMessageEntityCommand cmd) {
        event.publish(cmd);
    }
}
