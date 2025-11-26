package com.dev.lib.jpa.adapt;

import com.dev.lib.local.task.message.domain.adapter.event.LocalTaskMessageEvent;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTaskMessageAdapter implements LocalTaskMessageEvent {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(TaskMessageEntityCommand cmd) {
        publisher.publishEvent(new TaskMessageEntityCommand.Event(this, cmd));
    }
}
