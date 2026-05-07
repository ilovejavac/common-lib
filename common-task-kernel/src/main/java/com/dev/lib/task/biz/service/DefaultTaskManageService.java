package com.dev.lib.task.biz.service;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.task.api.ScheduleTriggerGateway;
import com.dev.lib.task.api.TaskManageService;
import com.dev.lib.task.biz.registry.TaskExecutorDescriptor;
import com.dev.lib.task.biz.registry.TaskExecutorRegistry;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.domain.TriggerSource;
import com.dev.lib.task.infra.data.TaskPayloadPo;
import com.dev.lib.task.infra.data.TaskPayloadRepository;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DefaultTaskManageService implements TaskManageService {

    private final TaskRepository taskRepository;

    private final TaskRunRepository taskRunRepository;

    private final TaskPayloadRepository taskPayloadRepository;

    private final ScheduleTriggerGateway scheduleTriggerGateway;

    private final TaskExecutorRegistry registry;

    public DefaultTaskManageService(
            TaskRepository taskRepository,
            TaskRunRepository taskRunRepository,
            TaskPayloadRepository taskPayloadRepository,
            ScheduleTriggerGateway scheduleTriggerGateway,
            TaskExecutorRegistry registry
    ) {

        this.taskRepository = taskRepository;
        this.taskRunRepository = taskRunRepository;
        this.taskPayloadRepository = taskPayloadRepository;
        this.scheduleTriggerGateway = scheduleTriggerGateway;
        this.registry = registry;
    }

    @Override
    @Transactional
    public void cancel(String taskId) {

        TaskPo task = mustLoad(taskId);
        task.setStatus(TaskStatus.CANCELED);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void retry(String taskId) {

        TaskPo task = mustLoad(taskId);
        if (task.getMode() == TaskMode.SCHEDULE) {
            throw new IllegalArgumentException("schedule task does not support retry(taskId)");
        }
        TaskRunPo latestRun = taskRunRepository.loadLatestByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("task run not found: " + taskId));

        TaskRunPo run = new TaskRunPo();
        run.setRunId(IDWorker.newId());
        run.setTaskId(taskId);
        run.setPayloadId(task.getPayloadId());
        run.setAttemptNo(latestRun.getAttemptNo() + 1);
        run.setStatus(TaskRunStatus.PENDING);
        run.setTriggerSource(TriggerSource.MANUAL);
        run.setTriggerTime(LocalDateTime.now());
        run.setScheduledAt(LocalDateTime.now());
        taskRunRepository.save(run);

        task.setStatus(TaskStatus.PENDING);
        task.setLastRunId(run.getRunId());
        task.setLastRunStatus(run.getStatus());
        task.setNextRunAt(run.getScheduledAt());
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void pause(String taskId) {

        TaskPo task = mustLoad(taskId);
        if (task.getMode() != TaskMode.SCHEDULE) {
            throw new IllegalArgumentException("only schedule task can be paused");
        }
        task.setStatus(TaskStatus.PAUSED);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public void resume(String taskId) {

        TaskPo task = mustLoad(taskId);
        if (task.getMode() != TaskMode.SCHEDULE) {
            throw new IllegalArgumentException("only schedule task can be resumed");
        }
        task.setStatus(TaskStatus.ENABLED);
        task.setNextRunAt(CronExpression.parse(task.getCron()).next(LocalDateTime.now()));
        taskRepository.save(task);
    }

    @Override
    public void triggerNow(String taskId) {

        scheduleTriggerGateway.fire(taskId, LocalDateTime.now(), TriggerSource.MANUAL);
    }

    @Override
    @Transactional
    public void refreshSchedule(String taskId) {

        TaskPo task = mustLoad(taskId);
        if (task.getMode() != TaskMode.SCHEDULE) {
            throw new IllegalArgumentException("only schedule task can be refreshed");
        }
        TaskPayloadPo payload = taskPayloadRepository.loadByPayloadId(task.getPayloadId())
                .orElseThrow(() -> new IllegalArgumentException("task payload not found: " + task.getPayloadId()));
        TaskExecutorDescriptor descriptor = registry.findByPayloadClassAndTaskType(resolveClass(payload.getPayloadClass()), task.getTaskType());
        if (descriptor == null) {
            throw new IllegalArgumentException("task executor not found for task: " + taskId);
        }
        task.setCron(descriptor.getCron());
        task.setRetryPolicyJson(com.dev.lib.util.Jsons.toJson(descriptor.getRetryPolicy()));
        task.setOnExhaustedStatus(descriptor.getOnExhaustedStatus());
        task.setConfigHash(Integer.toHexString((
                descriptor.getTaskType()
                        + "|" + descriptor.getMode()
                        + "|" + descriptor.getCron()
                        + "|" + com.dev.lib.util.Jsons.toJson(descriptor.getRetryPolicy())
                        + "|" + descriptor.getOnExhaustedStatus()
        ).hashCode()));
        task.setNextRunAt(CronExpression.parse(descriptor.getCron()).next(LocalDateTime.now()));
        taskRepository.save(task);
    }

    private TaskPo mustLoad(String taskId) {

        return taskRepository.loadByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("task not found: " + taskId));
    }

    private Class<?> resolveClass(String className) {

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("payload class not found: " + className, e);
        }
    }

}
