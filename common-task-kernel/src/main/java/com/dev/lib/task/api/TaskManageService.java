package com.dev.lib.task.api;

public interface TaskManageService {

    void cancel(String taskId);

    void retry(String taskId);

    void pause(String taskId);

    void resume(String taskId);

    void triggerNow(String taskId);

    void refreshSchedule(String taskId);

}
