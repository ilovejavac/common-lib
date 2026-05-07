package com.dev.lib.task.biz.launcher;

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
import com.dev.lib.util.Jsons;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class TaskSubmissionService {

    private final TaskExecutorRegistry registry;

    private final TaskRepository taskRepository;

    private final TaskPayloadRepository taskPayloadRepository;

    private final TaskRunRepository taskRunRepository;

    public TaskSubmissionService(
            TaskExecutorRegistry registry,
            TaskRepository taskRepository,
            TaskPayloadRepository taskPayloadRepository,
            TaskRunRepository taskRunRepository
    ) {

        this.registry = registry;
        this.taskRepository = taskRepository;
        this.taskPayloadRepository = taskPayloadRepository;
        this.taskRunRepository = taskRunRepository;
    }

    @Transactional
    public String submit(LocalDateTime runAt, String taskType, Object payload) {

        Objects.requireNonNull(payload, "payload must not be null");
        TaskExecutorDescriptor descriptor = resolveDescriptor(payload.getClass(), taskType);
        if (descriptor.requiresSchedule() && runAt != null) {
            throw new IllegalArgumentException("schedule task does not support runAt submission");
        }

        String taskId = newBizId();
        String payloadId = newBizId();
        String payloadJson = Jsons.toJson(payload);
        LocalDateTime now = LocalDateTime.now();

        TaskPo task = new TaskPo();
        task.setTaskId(taskId);
        task.setApplication(descriptor.getApplication());
        task.setTaskType(descriptor.getTaskType());
        task.setMode(descriptor.getMode());
        task.setPayloadId(payloadId);
        task.setCron(descriptor.getCron());
        task.setRetryPolicyJson(Jsons.toJson(descriptor.getRetryPolicy()));
        task.setOnExhaustedStatus(descriptor.getOnExhaustedStatus());
        task.setConfigHash(buildConfigHash(descriptor));

        TaskPayloadPo payloadPo = new TaskPayloadPo();
        payloadPo.setPayloadId(payloadId);
        payloadPo.setTaskId(taskId);
        payloadPo.setPayloadClass(payload.getClass().getName());
        payloadPo.setPayloadJson(payloadJson);
        payloadPo.setPayloadHash(Integer.toHexString(payloadJson.hashCode()));

        if (descriptor.getMode() == TaskMode.SCHEDULE) {
            task.setStatus(TaskStatus.ENABLED);
            task.setNextRunAt(nextCronTime(descriptor.getCron(), now));
            taskPayloadRepository.save(payloadPo);
            taskRepository.save(task);
            return taskId;
        }

        LocalDateTime scheduledAt = runAt == null ? now : runAt;
        task.setStatus(TaskStatus.PENDING);
        task.setRunAt(runAt);

        TaskRunPo run = new TaskRunPo();
        run.setRunId(newBizId());
        run.setTaskId(taskId);
        run.setPayloadId(payloadId);
        run.setStatus(TaskRunStatus.PENDING);
        run.setTriggerSource(TriggerSource.SUBMIT);
        run.setTriggerTime(now);
        run.setScheduledAt(scheduledAt);
        taskPayloadRepository.save(payloadPo);
        taskRunRepository.save(run);

        task.setLastRunId(run.getRunId());
        task.setLastRunStatus(run.getStatus());
        task.setNextRunAt(scheduledAt);
        taskRepository.save(task);
        return taskId;
    }

    private TaskExecutorDescriptor resolveDescriptor(Class<?> payloadClass, String taskType) {

        if (StringUtils.hasText(taskType)) {
            TaskExecutorDescriptor descriptor = registry.findByPayloadClassAndTaskType(payloadClass, taskType);
            if (descriptor == null) {
                throw new IllegalArgumentException(
                        "No task executor found for payload " + payloadClass.getName() + " and taskType " + taskType
                );
            }
            return descriptor;
        }

        List<TaskExecutorDescriptor> descriptors = registry.findByPayloadClass(payloadClass);
        if (descriptors.isEmpty()) {
            throw new IllegalArgumentException("No task executor found for payload " + payloadClass.getName());
        }
        if (descriptors.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple task executors found for payload " + payloadClass.getName() + ", please specify taskType"
            );
        }
        return descriptors.get(0);
    }

    private LocalDateTime nextCronTime(String cron, LocalDateTime now) {

        return CronExpression.parse(cron).next(now);
    }

    private String buildConfigHash(TaskExecutorDescriptor descriptor) {

        return Integer.toHexString((
                descriptor.getTaskType()
                        + "|" + descriptor.getMode()
                        + "|" + descriptor.getCron()
                        + "|" + Jsons.toJson(descriptor.getRetryPolicy())
                        + "|" + descriptor.getOnExhaustedStatus()
        ).hashCode());
    }

    private String newBizId() {

        return com.dev.lib.entity.id.IDWorker.newId();
    }

}
