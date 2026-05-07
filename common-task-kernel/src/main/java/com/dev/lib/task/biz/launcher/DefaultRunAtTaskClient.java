package com.dev.lib.task.biz.launcher;

import com.dev.lib.task.api.RunAtTaskClient;

import java.time.LocalDateTime;

public class DefaultRunAtTaskClient implements RunAtTaskClient {

    private final TaskSubmissionService submissionService;

    private final LocalDateTime runAt;

    public DefaultRunAtTaskClient(TaskSubmissionService submissionService, LocalDateTime runAt) {

        this.submissionService = submissionService;
        this.runAt = runAt;
    }

    @Override
    public String submit(Object payload) {

        return submissionService.submit(runAt, null, payload);
    }

    @Override
    public String submit(String taskType, Object payload) {

        return submissionService.submit(runAt, taskType, payload);
    }

}
