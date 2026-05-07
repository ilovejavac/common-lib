package com.dev.lib.task.biz.launcher;

import com.dev.lib.task.api.RunAtTaskClient;
import com.dev.lib.task.api.TaskClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DefaultTaskClient implements TaskClient {

    private final TaskSubmissionService submissionService;

    public DefaultTaskClient(TaskSubmissionService submissionService) {

        this.submissionService = submissionService;
    }

    @Override
    public String submit(Object payload) {

        return submissionService.submit(null, null, payload);
    }

    @Override
    public String submit(String taskType, Object payload) {

        return submissionService.submit(null, taskType, payload);
    }

    @Override
    public RunAtTaskClient runAt(LocalDateTime runAt) {

        return new DefaultRunAtTaskClient(submissionService, runAt);
    }

}
