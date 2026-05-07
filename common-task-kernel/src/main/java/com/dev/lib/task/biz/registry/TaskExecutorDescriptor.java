package com.dev.lib.task.biz.registry;

import com.dev.lib.task.domain.RetryPolicy;
import com.dev.lib.task.domain.TaskExecutor;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskExecutorDescriptor {

    private final String application;

    private final String taskType;

    private final Class<?> payloadClass;

    private final TaskMode mode;

    private final String cron;

    private final RetryPolicy retryPolicy;

    private final TaskStatus onExhaustedStatus;

    private final TaskExecutor<?> executor;

    public boolean requiresSchedule() {

        return mode == TaskMode.SCHEDULE;
    }

}
