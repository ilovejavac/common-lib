package com.dev.lib.local.task.message.domain.adapter;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.local.task.message.data.LocalTaskMessagePoToTaskMessageEntityCommandMapper;
import com.dev.lib.local.task.message.data.LocalTaskStatus;
import com.dev.lib.local.task.message.data.TaskMessageRepository;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommandToLocalTaskMessagePoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(TaskMessageEntityCommand cmd) {
        var po = commandToLocalTaskMessagePoMapper.convert(cmd);
        po.setId(IDWorker.nextID());
        po.setHouseNumber(Long.hashCode(po.getId()) % 10);
        po.setStatus(LocalTaskStatus.PENDING);
        po.setRetryCount(0);
        repository.save(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatusToSuccess(String taskId) {
        updateStatus(taskId, LocalTaskStatus.SUCCESS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatusToFailed(String taskId) {
        updateStatus(taskId, LocalTaskStatus.FAILED);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTaskStatusToProcessing(String taskId) {
        repository.loadById(taskId).ifPresent(it -> it.setStatus(LocalTaskStatus.PROCESSING));
    }

    private void updateStatus(String taskId, LocalTaskStatus status) {
        repository.loadById(taskId).ifPresent(it -> {
            it.setStatus(status);
            it.setProcessedAt(java.time.LocalDateTime.now());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskMessageEntityCommand> selectByHouseNumber(
            List<Integer> houseNumbers,
            String taskId,
            Integer limit
    ) {
        Long lastId = Optional.ofNullable(taskId)
                .filter(id -> !id.isEmpty())
                .flatMap(repository::loadById)
                .map(po -> po.getId())
                .orElse(null);

        return repository.loadsByHouseNumber(houseNumbers, lastId, limit).stream()
                .map(taskMessageEntityCommandMapper::convert)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String selectMinIdByHouseNumber(List<Integer> houseNumbers) {
        return repository.loadsByHouseNumber(houseNumbers, null, 1)
                .stream()
                .findFirst()
                .map(po -> po.getTaskId())
                .orElse("");
    }
}
