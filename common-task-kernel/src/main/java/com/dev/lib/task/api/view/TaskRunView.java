package com.dev.lib.task.api.view;

import com.dev.lib.task.domain.TaskRunStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskRunView {

    private String runId;

    private Integer attemptNo;

    private TaskRunStatus status;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private String errorMessage;

}
