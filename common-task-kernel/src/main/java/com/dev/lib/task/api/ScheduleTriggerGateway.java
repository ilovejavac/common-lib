package com.dev.lib.task.api;

import com.dev.lib.task.domain.TriggerSource;

import java.time.LocalDateTime;

public interface ScheduleTriggerGateway {

    void fire(String taskId, LocalDateTime triggerTime, TriggerSource triggerSource);

}
