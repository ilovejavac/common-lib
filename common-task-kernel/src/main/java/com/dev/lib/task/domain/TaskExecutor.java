package com.dev.lib.task.domain;

public interface TaskExecutor<T> {

    TaskExecuteResult execute(TaskExecuteContext<T> context) throws Exception;

}
