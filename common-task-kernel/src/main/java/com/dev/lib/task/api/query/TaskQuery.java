package com.dev.lib.task.api.query;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.infra.data.TaskPo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQuery extends DslQuery<TaskPo> {

    private String taskType;

    private TaskMode mode;

    private TaskStatus status;

}
