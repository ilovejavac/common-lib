package com.dev.lib.task.biz.registry;

import com.dev.lib.task.annotation.Task;
import com.dev.lib.task.domain.AsyncTaskExecutor;
import com.dev.lib.task.domain.ReliableTaskExecutor;
import com.dev.lib.task.domain.RetryPolicy;
import com.dev.lib.task.domain.ScheduleTaskExecutor;
import com.dev.lib.task.domain.TaskExecutor;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TaskExecutorRegistry {

    private final List<TaskExecutor<?>> executors;

    private final String application;

    private final Map<Class<?>, List<TaskExecutorDescriptor>> descriptorsByPayload = new LinkedHashMap<>();

    private final Map<String, TaskExecutorDescriptor> descriptorsByPayloadAndType = new LinkedHashMap<>();

    public TaskExecutorRegistry(
            List<TaskExecutor<?>> executors,
            @Value("${spring.application.name:application}") String application
    ) {

        this.executors = executors;
        this.application = application;
    }

    @PostConstruct
    public void init() {

        for (TaskExecutor<?> executor : executors) {
            TaskExecutorDescriptor descriptor = buildDescriptor(executor);
            descriptorsByPayload.computeIfAbsent(descriptor.getPayloadClass(), key -> new ArrayList<>())
                    .add(descriptor);

            if (StringUtils.hasText(descriptor.getTaskType())) {
                String key = descriptor.getPayloadClass().getName() + "#" + descriptor.getTaskType();
                if (descriptorsByPayloadAndType.putIfAbsent(key, descriptor) != null) {
                    throw new IllegalStateException("Duplicate task executor registration for key: " + key);
                }
            }
        }
    }

    public List<TaskExecutorDescriptor> findByPayloadClass(Class<?> payloadClass) {

        return descriptorsByPayload.getOrDefault(payloadClass, List.of());
    }

    public TaskExecutorDescriptor findByPayloadClassAndTaskType(Class<?> payloadClass, String taskType) {

        return descriptorsByPayloadAndType.get(payloadClass.getName() + "#" + taskType);
    }

    private TaskExecutorDescriptor buildDescriptor(TaskExecutor<?> executor) {

        Class<?> targetClass = AopUtils.getTargetClass(executor);
        Task annotation = targetClass.getAnnotation(Task.class);
        String taskType = annotation == null || !StringUtils.hasText(annotation.value())
                ? targetClass.getSimpleName()
                : annotation.value();
        String cron = annotation == null ? "" : annotation.cron();
        TaskMode mode = resolveMode(targetClass);
        if (mode == TaskMode.SCHEDULE && !StringUtils.hasText(cron)) {
            throw new IllegalStateException("Schedule task executor must declare cron: " + targetClass.getName());
        }

        return TaskExecutorDescriptor.builder()
                .application(application)
                .taskType(taskType)
                .payloadClass(resolvePayloadClass(targetClass))
                .mode(mode)
                .cron(cron)
                .retryPolicy(resolveRetryPolicy(targetClass))
                .onExhaustedStatus(resolveOnExhaustedStatus(targetClass))
                .executor(executor)
                .build();
    }

    private TaskMode resolveMode(Class<?> targetClass) {

        if (ScheduleTaskExecutor.class.isAssignableFrom(targetClass)) {
            return TaskMode.SCHEDULE;
        }
        return TaskMode.ONCE;
    }

    private RetryPolicy resolveRetryPolicy(Class<?> targetClass) {

        if (ReliableTaskExecutor.class.isAssignableFrom(targetClass)) {
            return RetryPolicy.builder()
                    .maxRetry(10)
                    .initialDelay(Duration.ofSeconds(2))
                    .exponentialBackoff(Boolean.TRUE)
                    .build();
        }

        return RetryPolicy.builder()
                .maxRetry(2)
                .fixedDelay(Duration.ofSeconds(10))
                .exponentialBackoff(Boolean.FALSE)
                .build();
    }

    private TaskStatus resolveOnExhaustedStatus(Class<?> targetClass) {

        if (ReliableTaskExecutor.class.isAssignableFrom(targetClass)) {
            return TaskStatus.MANUAL_REQUIRED;
        }
        return TaskStatus.FAILED;
    }

    private Class<?> resolvePayloadClass(Class<?> targetClass) {

        ResolvableType type = ResolvableType.forClass(targetClass).as(TaskExecutor.class);
        Class<?> payloadClass = type.resolveGeneric(0);
        if (payloadClass == null || payloadClass == Object.class) {
            throw new IllegalStateException("Unable to resolve payload class for executor: " + targetClass.getName());
        }
        return payloadClass;
    }

}
