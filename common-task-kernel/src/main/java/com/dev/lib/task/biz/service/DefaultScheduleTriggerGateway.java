package com.dev.lib.task.biz.service;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.task.api.ScheduleTriggerGateway;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.domain.TriggerSource;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DefaultScheduleTriggerGateway implements ScheduleTriggerGateway {

    private final TaskRepository taskRepository;

    private final TaskRunRepository taskRunRepository;

    public DefaultScheduleTriggerGateway(TaskRepository taskRepository, TaskRunRepository taskRunRepository) {

        this.taskRepository = taskRepository;
        this.taskRunRepository = taskRunRepository;
    }

    @Override
    @Transactional
    public void fire(String taskId, LocalDateTime triggerTime, TriggerSource triggerSource) {

        TaskPo task = taskRepository.loadByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("task not found: " + taskId));
        if (task.getMode() != TaskMode.SCHEDULE) {
            throw new IllegalArgumentException("task is not a schedule task: " + taskId);
        }
        if (task.getStatus() != TaskStatus.ENABLED) {
            return;
        }
        if (taskRunRepository.existsByTaskIdAndTriggerTime(taskId, triggerTime)) {
            return;
        }

        TaskRunPo run = new TaskRunPo();
        run.setRunId(IDWorker.newId());
        run.setTaskId(taskId);
        run.setPayloadId(task.getPayloadId());
        run.setStatus(TaskRunStatus.PENDING);
        run.setTriggerSource(triggerSource);
        run.setTriggerTime(triggerTime);
        run.setScheduledAt(triggerTime);
        taskRunRepository.save(run);

        task.setLastRunId(run.getRunId());
        task.setLastRunStatus(run.getStatus());
        taskRepository.save(task);
    }

}
