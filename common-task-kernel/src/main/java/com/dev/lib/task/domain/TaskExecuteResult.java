package com.dev.lib.task.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskExecuteResult {

    private final boolean success;

    private final boolean retryable;

    private final Object result;

    private final String errorMessage;

    public static TaskExecuteResult success() {

        return new TaskExecuteResult(true, false, null, null);
    }

    public static TaskExecuteResult success(Object result) {

        return new TaskExecuteResult(true, false, result, null);
    }

    public static TaskExecuteResult failure(String errorMessage) {

        return new TaskExecuteResult(false, true, null, errorMessage);
    }

    public static TaskExecuteResult failureNoRetry(String errorMessage) {

        return new TaskExecuteResult(false, false, null, errorMessage);
    }

}
