package com.dev.lib.task.api;

import java.time.LocalDateTime;

public interface TaskClient {

    String submit(Object payload);

    String submit(String taskType, Object payload);

    RunAtTaskClient runAt(LocalDateTime runAt);

}
