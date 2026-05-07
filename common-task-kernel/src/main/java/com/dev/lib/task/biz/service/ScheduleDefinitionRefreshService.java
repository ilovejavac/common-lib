package com.dev.lib.task.biz.service;

import com.dev.lib.task.biz.registry.TaskExecutorDescriptor;
import com.dev.lib.task.biz.registry.TaskExecutorRegistry;
import com.dev.lib.task.infra.data.TaskPayloadPo;
import com.dev.lib.task.infra.data.TaskPayloadRepository;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.util.Jsons;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ScheduleDefinitionRefreshService {

    private final TaskRepository taskRepository;

    private final TaskPayloadRepository taskPayloadRepository;

    private final TaskExecutorRegistry registry;

    public ScheduleDefinitionRefreshService(
            TaskRepository taskRepository,
            TaskPayloadRepository taskPayloadRepository,
            TaskExecutorRegistry registry
    ) {

        this.taskRepository = taskRepository;
        this.taskPayloadRepository = taskPayloadRepository;
        this.registry = registry;
    }

    @PostConstruct
    public void refreshSchedules() {

        for (TaskPo task : taskRepository.loadsSchedules()) {
            TaskPayloadPo payload = taskPayloadRepository.loadByPayloadId(task.getPayloadId()).orElse(null);
            if (payload == null) {
                continue;
            }
            TaskExecutorDescriptor descriptor = registry.findByPayloadClassAndTaskType(resolveClass(payload.getPayloadClass()), task.getTaskType());
            if (descriptor == null || descriptor.getCron() == null || descriptor.getCron().isBlank()) {
                continue;
            }
            String configHash = Integer.toHexString((
                    descriptor.getTaskType()
                            + "|" + descriptor.getMode()
                            + "|" + descriptor.getCron()
                            + "|" + Jsons.toJson(descriptor.getRetryPolicy())
                            + "|" + descriptor.getOnExhaustedStatus()
            ).hashCode());
            if (configHash.equals(task.getConfigHash())) {
                continue;
            }
            task.setCron(descriptor.getCron());
            task.setRetryPolicyJson(Jsons.toJson(descriptor.getRetryPolicy()));
            task.setOnExhaustedStatus(descriptor.getOnExhaustedStatus());
            task.setConfigHash(configHash);
            task.setNextRunAt(CronExpression.parse(descriptor.getCron()).next(LocalDateTime.now()));
            taskRepository.save(task);
        }
    }

    private Class<?> resolveClass(String className) {

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("payload class not found: " + className, e);
        }
    }

}
