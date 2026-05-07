package com.dev.lib.task.biz.dispatcher;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.task.biz.registry.TaskExecutorDescriptor;
import com.dev.lib.task.biz.registry.TaskExecutorRegistry;
import com.dev.lib.task.domain.TaskExecuteContext;
import com.dev.lib.task.domain.TaskExecuteResult;
import com.dev.lib.task.domain.TaskExecutor;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.domain.TriggerSource;
import com.dev.lib.task.infra.data.TaskPayloadPo;
import com.dev.lib.task.infra.data.TaskPayloadRepository;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import com.dev.lib.util.Jsons;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskExecutionService {

    private final TaskRunRepository taskRunRepository;

    private final TaskRepository taskRepository;

    private final TaskPayloadRepository taskPayloadRepository;

    private final TaskExecutorRegistry registry;

    private final String workerId = "worker-" + IDWorker.newId();

    public TaskExecutionService(
            TaskRunRepository taskRunRepository,
            TaskRepository taskRepository,
            TaskPayloadRepository taskPayloadRepository,
            TaskExecutorRegistry registry
    ) {

        this.taskRunRepository = taskRunRepository;
        this.taskRepository = taskRepository;
        this.taskPayloadRepository = taskPayloadRepository;
        this.registry = registry;
    }

    @Transactional
    public List<TaskRunPo> claimRuns(LocalDateTime now, int limit) {

        List<TaskRunPo> runs = taskRunRepository.loadsRunnableForUpdate(now, limit);
        for (TaskRunPo run : runs) {
            run.setStatus(TaskRunStatus.RUNNING);
            run.setWorkerId(workerId);
            run.setStartedAt(now);
            run.setHeartbeatAt(now);
            run.setLeaseExpireAt(now.plusMinutes(10));
            taskRunRepository.save(run);

            TaskPo task = taskRepository.loadByTaskId(run.getTaskId()).orElse(null);
            if (task != null && task.getMode() == TaskMode.ONCE) {
                task.setStatus(TaskStatus.RUNNING);
                task.setLastRunId(run.getRunId());
                task.setLastRunStatus(run.getStatus());
                taskRepository.save(task);
            }
        }
        return runs;
    }

    public void execute(String runId) {

        TaskRunPo run = taskRunRepository.loadByRunId(runId).orElse(null);
        if (run == null) {
            return;
        }
        TaskPo task = taskRepository.loadByTaskId(run.getTaskId()).orElse(null);
        if (task == null) {
            return;
        }
        TaskPayloadPo payloadPo = taskPayloadRepository.loadByPayloadId(run.getPayloadId()).orElse(null);
        if (payloadPo == null) {
            failRun(runId, "task payload not found", null, false);
            return;
        }

        TaskExecutorDescriptor descriptor = registry.findByPayloadClassAndTaskType(resolveClass(payloadPo.getPayloadClass()), task.getTaskType());
        if (descriptor == null) {
            failRun(runId, "task executor not found", null, false);
            return;
        }

        try {
            Object payload = Jsons.parse(payloadPo.getPayloadJson(), descriptor.getPayloadClass());
            @SuppressWarnings("unchecked")
            TaskExecutor<Object> executor = (TaskExecutor<Object>) descriptor.getExecutor();
            TaskExecuteResult result = executor.execute(TaskExecuteContext.<Object>builder()
                    .taskId(task.getTaskId())
                    .runId(run.getRunId())
                    .taskType(task.getTaskType())
                    .payload(payload)
                    .attemptNo(run.getAttemptNo())
                    .triggerTime(run.getTriggerTime())
                    .build());
            if (result.isSuccess()) {
                succeedRun(runId, result);
            } else {
                failRun(runId, result.getErrorMessage(), null, result.isRetryable());
            }
        } catch (Exception e) {
            failRun(runId, e.getMessage(), stackTraceOf(e), true);
        }
    }

    protected void succeedRun(String runId, TaskExecuteResult result) {

        TaskRunPo run = taskRunRepository.loadByRunId(runId).orElseThrow();
        TaskPo task = taskRepository.loadByTaskId(run.getTaskId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        run.setStatus(TaskRunStatus.SUCCESS);
        run.setFinishedAt(now);
        run.setErrorMessage(null);
        run.setErrorStack(null);
        taskRunRepository.save(run);

        task.setLastRunId(run.getRunId());
        task.setLastRunStatus(run.getStatus());
        task.setLastRunAt(now);
        task.setErrorMessage(null);
        if (task.getMode() == TaskMode.ONCE) {
            task.setStatus(TaskStatus.SUCCESS);
            task.setNextRunAt(null);
        }
        taskRepository.save(task);
    }

    protected void failRun(String runId, String errorMessage, String errorStack, boolean retryable) {

        TaskRunPo run = taskRunRepository.loadByRunId(runId).orElseThrow();
        TaskPo task = taskRepository.loadByTaskId(run.getTaskId()).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        run.setStatus(TaskRunStatus.FAILED);
        run.setFinishedAt(now);
        run.setErrorMessage(errorMessage);
        run.setErrorStack(errorStack);
        taskRunRepository.save(run);

        task.setLastRunId(run.getRunId());
        task.setLastRunStatus(run.getStatus());
        task.setLastRunAt(now);
        task.setErrorMessage(errorMessage);

        RetryDecision decision = RetryDecision.next(task.getRetryPolicyJson(), run.getAttemptNo(), retryable);
        if (task.getMode() == TaskMode.SCHEDULE) {
            taskRepository.save(task);
            return;
        }

        if (decision.shouldRetry()) {
            TaskRunPo next = new TaskRunPo();
            next.setRunId(IDWorker.newId());
            next.setTaskId(task.getTaskId());
            next.setPayloadId(run.getPayloadId());
            next.setAttemptNo(run.getAttemptNo() + 1);
            next.setStatus(TaskRunStatus.PENDING);
            next.setTriggerSource(TriggerSource.RETRY);
            next.setTriggerTime(now);
            next.setScheduledAt(decision.nextTime());
            next.setNextRetryAt(decision.nextTime());
            taskRunRepository.save(next);

            task.setStatus(TaskStatus.PENDING);
            task.setNextRunAt(decision.nextTime());
            task.setLastRunId(next.getRunId());
            task.setLastRunStatus(next.getStatus());
        } else {
            task.setStatus(task.getOnExhaustedStatus());
            task.setNextRunAt(null);
        }
        taskRepository.save(task);
    }

    private Class<?> resolveClass(String className) {

        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("payload class not found: " + className, e);
        }
    }

    private String stackTraceOf(Exception e) {

        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

}
