package com.dev.lib.task.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TaskExecuteContext<T> {

    private final String taskId;

    private final String runId;

    private final String taskType;

    private final T payload;

    private final Integer attemptNo;

    private final LocalDateTime triggerTime;

}
