package com.dev.lib.task.schedule.internal;

import com.dev.lib.task.api.ScheduleTriggerGateway;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.dev.lib.task.domain.TriggerSource.SCHEDULE;

@Component
public class InternalScheduleTriggerJob {

    private final TaskRepository taskRepository;

    private final ScheduleTriggerGateway triggerGateway;

    public InternalScheduleTriggerJob(TaskRepository taskRepository, ScheduleTriggerGateway triggerGateway) {

        this.taskRepository = taskRepository;
        this.triggerGateway = triggerGateway;
    }

    @Scheduled(fixedDelayString = "${app.task.schedule-trigger-delay:10000}")
    public void triggerDueSchedules() {

        LocalDateTime now = LocalDateTime.now();
        for (TaskPo task : taskRepository.loadsDueSchedules(now, 100)) {
            triggerGateway.fire(task.getTaskId(), task.getNextRunAt(), SCHEDULE);
            refreshNextRunAt(task, now);
        }
    }

    protected void refreshNextRunAt(TaskPo task, LocalDateTime baseTime) {

        TaskPo latest = taskRepository.loadByTaskId(task.getTaskId()).orElse(null);
        if (latest == null || latest.getCron() == null || latest.getStatus() != TaskStatus.ENABLED) {
            return;
        }
        latest.setNextRunAt(CronExpression.parse(latest.getCron()).next(baseTime));
        taskRepository.save(latest);
    }

}
