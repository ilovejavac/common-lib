package com.dev.lib.local.task.message.trigger.listener;

import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.service.ITaskNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageEventListener {
    private final ITaskNotifyService notifyService;

    @Async
    @EventListener
    public void onTaskMessageEvent(TaskMessageEntityCommand.Event event) {
        try {
            log.info("receive info: {} {}", event.getCmd(), event.getTimestamp());
            log.info("通知结果 {}", notifyService.notify(event.getCmd()));
        } catch (Exception e) {
            log.error("处理任务消息是件失败 {}", event, e);
        }
    }
}
