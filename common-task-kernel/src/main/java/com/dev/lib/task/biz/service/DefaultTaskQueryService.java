package com.dev.lib.task.biz.service;

import com.dev.lib.task.api.TaskQueryService;
import com.dev.lib.task.api.query.TaskQuery;
import com.dev.lib.task.api.view.TaskRunView;
import com.dev.lib.task.api.view.TaskView;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultTaskQueryService implements TaskQueryService {

    private final TaskRepository taskRepository;

    private final TaskRunRepository taskRunRepository;

    public DefaultTaskQueryService(TaskRepository taskRepository, TaskRunRepository taskRunRepository) {

        this.taskRepository = taskRepository;
        this.taskRunRepository = taskRunRepository;
    }

    @Override
    public TaskView getTask(String taskId) {

        return taskRepository.loadByTaskId(taskId).map(this::toView).orElse(null);
    }

    @Override
    public Page<TaskView> pageTasks(TaskQuery query) {

        Page<TaskPo> page = taskRepository.page(query);
        List<TaskView> content = page.getContent().stream().map(this::toView).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    @Override
    public List<TaskRunView> listRuns(String taskId) {

        return taskRunRepository.loadsByTaskId(taskId).stream().map(this::toView).toList();
    }

    @Override
    public TaskRunView getLastRun(String taskId) {

        return taskRunRepository.loadLatestByTaskId(taskId).map(this::toView).orElse(null);
    }

    private TaskView toView(TaskPo po) {

        TaskView view = new TaskView();
        view.setTaskId(po.getTaskId());
        view.setTaskType(po.getTaskType());
        view.setMode(po.getMode());
        view.setStatus(po.getStatus());
        view.setLastRunStatus(po.getLastRunStatus());
        view.setCreatedAt(po.getCreatedAt());
        view.setUpdatedAt(po.getUpdatedAt());
        view.setLastRunAt(po.getLastRunAt());
        view.setNextRunAt(po.getNextRunAt());
        view.setErrorMessage(po.getErrorMessage());
        return view;
    }

    private TaskRunView toView(TaskRunPo po) {

        TaskRunView view = new TaskRunView();
        view.setRunId(po.getRunId());
        view.setAttemptNo(po.getAttemptNo());
        view.setStatus(po.getStatus());
        view.setScheduledAt(po.getScheduledAt());
        view.setStartedAt(po.getStartedAt());
        view.setFinishedAt(po.getFinishedAt());
        view.setErrorMessage(po.getErrorMessage());
        return view;
    }

}
