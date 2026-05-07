package com.dev.lib.task.api;

import com.dev.lib.task.api.query.TaskQuery;
import com.dev.lib.task.api.view.TaskRunView;
import com.dev.lib.task.api.view.TaskView;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TaskQueryService {

    TaskView getTask(String taskId);

    Page<TaskView> pageTasks(TaskQuery query);

    List<TaskRunView> listRuns(String taskId);

    TaskRunView getLastRun(String taskId);

}
