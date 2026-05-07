package com.dev.lib.task.biz.dispatcher;

import com.dev.lib.task.infra.data.TaskRunPo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TaskDispatcher {

    private final TaskExecutionService taskExecutionService;

    public TaskDispatcher(TaskExecutionService taskExecutionService) {

        this.taskExecutionService = taskExecutionService;
    }

    @Scheduled(fixedDelayString = "${app.task.dispatch-delay:5000}")
    public void dispatch() {

        List<TaskRunPo> runs = taskExecutionService.claimRuns(LocalDateTime.now(), 50);
        for (TaskRunPo run : runs) {
            taskExecutionService.execute(run.getRunId());
        }
    }

}
