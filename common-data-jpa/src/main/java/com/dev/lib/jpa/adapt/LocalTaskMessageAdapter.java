package com.dev.lib.jpa.adapt;

import com.dev.lib.jpa.infra.localTaskMessage.LocalTaskMessagePo;
import com.dev.lib.jpa.infra.localTaskMessage.LocalTaskMessagePoToTaskMessageEntityCommandMapper;
import com.dev.lib.jpa.infra.localTaskMessage.LocalTaskStatus;
import com.dev.lib.jpa.infra.localTaskMessage.TaskMessageRepository;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageAdapt;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageEvent;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommandToLocalTaskMessagePoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTaskMessageAdapter implements ILocalTaskMessageEvent, ILocalTaskMessageAdapt {

    private final ApplicationEventPublisher publisher;
    private final TaskMessageRepository repository;
    private final TaskMessageEntityCommandToLocalTaskMessagePoMapper commandToLocalTaskMessagePoMapper;
    private final LocalTaskMessagePoToTaskMessageEntityCommandMapper taskMessageEntityCommandMapper;

    @Override
    public void publish(TaskMessageEntityCommand cmd) {
        publisher.publishEvent(new TaskMessageEntityCommand.Event(this, cmd));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(TaskMessageEntityCommand cmd) {
        LocalTaskMessagePo po = repository.save(commandToLocalTaskMessagePoMapper.convert(cmd));
        po.setHouseNumber(po.getId().hashCode() % 10);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatusToSuccess(String taskId) {
        repository.loadById(taskId).ifPresent(it -> it.setStatus(LocalTaskStatus.SUCCESS));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatusToFailed(String taskId) {
        repository.loadById(taskId).ifPresent(it -> it.setStatus(LocalTaskStatus.FAILED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskMessageEntityCommand> selectByHouseNumber(
            List<Integer> houseNumbers,
            String taskId,
            Integer limit
    ) {
        Optional<LocalTaskMessagePo> loadtask = repository.loadById(taskId);
        return loadtask.map(localTaskMessagePo -> repository.loadsByHouseNumber(
                                houseNumbers,
                                localTaskMessagePo.getId(),
                                limit
                        )
                        .stream().map(taskMessageEntityCommandMapper::convert).toList())
                .orElseGet(ArrayList::new);
    }

    @Override
    @Transactional(readOnly = true)
    public String selectMinIdByHouseNumber(List<Integer> houseNumbers) {
        return repository.loadsByHouseNumber(houseNumbers, null, 2).stream()
                .findFirst()
                .map(taskMessageEntityCommandMapper::convert)
                .map(TaskMessageEntityCommand::getTaskId)
                .orElse("-1");
    }
}
