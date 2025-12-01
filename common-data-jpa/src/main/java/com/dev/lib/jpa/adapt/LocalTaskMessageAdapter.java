package com.dev.lib.jpa.adapt;

import com.dev.lib.jpa.infra.localTaskMessage.LocalTaskMessagePo;
import com.dev.lib.jpa.infra.localTaskMessage.TaskMessageRepository;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageEvent;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommandToLocalTaskMessagePoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTaskMessageAdapter implements ILocalTaskMessageEvent {

    private final ApplicationEventPublisher publisher;
    private final TaskMessageRepository repository;
    private final TaskMessageEntityCommandToLocalTaskMessagePoMapper commandToLocalTaskMessagePoMapper;

    @Override
    public void publish(TaskMessageEntityCommand cmd) {
        publisher.publishEvent(new TaskMessageEntityCommand.Event(this, cmd));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(TaskMessageEntityCommand cmd) {
        LocalTaskMessagePo po = repository.save(commandToLocalTaskMessagePoMapper.convert(cmd));
        po.setHouseNumber(po.getId().hashCode() % 10);
    }
}
