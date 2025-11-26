package com.dev.lib.local.task.message.listener;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageEventListener {

    @Async
    @EventListener
    public void onTaskMessageEvent(TaskMessageEntityCommand.Event event) {
        log.info("receive info: {} {}", event.getCmd(), event.getTimestamp());
    }
}
