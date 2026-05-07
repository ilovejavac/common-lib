package com.dev.lib.task.biz.recovery;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.task.biz.dispatcher.RetryDecision;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.domain.TriggerSource;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import com.dev.lib.task.infra.data.TaskPayloadPo;
import com.dev.lib.task.infra.data.TaskPayloadRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TaskRecoveryService {

    private final TaskRunRepository taskRunRepository;

    private final TaskRepository taskRepository;

    private final TaskPayloadRepository taskPayloadRepository;

    public TaskRecoveryService(
            TaskRunRepository taskRunRepository,
            TaskRepository taskRepository,
            TaskPayloadRepository taskPayloadRepository
    ) {

        this.taskRunRepository = taskRunRepository;
        this.taskRepository = taskRepository;
        this.taskPayloadRepository = taskPayloadRepository;
    }

    @PostConstruct
    public void recover() {

        for (TaskRunPo run : taskRunRepository.loadsExpiredRunning(LocalDateTime.now(), 200)) {
            recover(run.getRunId());
        }
    }

    protected void recover(String runId) {

        TaskRunPo run = taskRunRepository.loadByRunId(runId).orElse(null);
        if (run == null || run.getStatus() != TaskRunStatus.RUNNING) {
            return;
        }
        TaskPo task = taskRepository.loadByTaskId(run.getTaskId()).orElse(null);
        if (task == null) {
            return;
        }

        run.setStatus(TaskRunStatus.INTERRUPTED);
        run.setFinishedAt(LocalDateTime.now());
        taskRunRepository.save(run);

        if (task.getMode() == TaskMode.SCHEDULE) {
            return;
        }

        RetryDecision decision = RetryDecision.next(task.getRetryPolicyJson(), run.getAttemptNo(), true);
        if (decision.shouldRetry()) {
            TaskRunPo next = new TaskRunPo();
            next.setRunId(IDWorker.newId());
            next.setTaskId(task.getTaskId());
            next.setPayloadId(run.getPayloadId());
            next.setAttemptNo(run.getAttemptNo() + 1);
            next.setStatus(TaskRunStatus.PENDING);
            next.setTriggerSource(TriggerSource.RECOVERY);
            next.setTriggerTime(LocalDateTime.now());
            next.setScheduledAt(decision.nextTime());
            next.setNextRetryAt(decision.nextTime());
            taskRunRepository.save(next);

            task.setStatus(TaskStatus.PENDING);
            task.setLastRunId(next.getRunId());
            task.setLastRunStatus(next.getStatus());
            task.setNextRunAt(decision.nextTime());
        } else {
            task.setStatus(task.getOnExhaustedStatus());
            task.setNextRunAt(null);
        }
        taskRepository.save(task);
    }

}
