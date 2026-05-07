package com.dev.lib.task.api;

public interface RunAtTaskClient {

    String submit(Object payload);

    String submit(String taskType, Object payload);

}
