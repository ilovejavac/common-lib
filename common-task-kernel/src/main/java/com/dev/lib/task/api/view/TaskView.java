package com.dev.lib.task.api.view;

import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskView {

    private String taskId;

    private String taskType;

    private TaskMode mode;

    private TaskStatus status;

    private TaskRunStatus lastRunStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastRunAt;

    private LocalDateTime nextRunAt;

    private String errorMessage;

}
