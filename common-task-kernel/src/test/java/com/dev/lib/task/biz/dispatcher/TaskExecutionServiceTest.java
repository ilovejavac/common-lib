package com.dev.lib.task.biz.dispatcher;

import com.dev.lib.task.biz.registry.TaskExecutorDescriptor;
import com.dev.lib.task.biz.registry.TaskExecutorRegistry;
import com.dev.lib.task.domain.RetryPolicy;
import com.dev.lib.task.domain.TaskExecuteContext;
import com.dev.lib.task.domain.TaskExecuteResult;
import com.dev.lib.task.domain.TaskExecutor;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.infra.data.TaskPayloadPo;
import com.dev.lib.task.infra.data.TaskPayloadRepository;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import com.dev.lib.util.Jsons;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceTest {

    @Mock
    private TaskRunRepository taskRunRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskPayloadRepository taskPayloadRepository;

    @Mock
    private TaskExecutorRegistry registry;

    @Test
    void shouldMarkOnceTaskSuccessWhenExecutorSucceeds() {

        TaskRunPo run = new TaskRunPo();
        run.setRunId("run-1");
        run.setTaskId("task-1");
        run.setPayloadId("payload-1");
        run.setAttemptNo(1);

        TaskPo task = new TaskPo();
        task.setTaskId("task-1");
        task.setTaskType("EXPORT");
        task.setMode(TaskMode.ONCE);
        task.setOnExhaustedStatus(TaskStatus.FAILED);

        TaskPayloadPo payloadPo = new TaskPayloadPo();
        payloadPo.setPayloadId("payload-1");
        payloadPo.setPayloadClass(Payload.class.getName());
        payloadPo.setPayloadJson(Jsons.toJson(new Payload("ok")));

        TaskExecutorDescriptor descriptor = TaskExecutorDescriptor.builder()
                .taskType("EXPORT")
                .payloadClass(Payload.class)
                .mode(TaskMode.ONCE)
                .retryPolicy(RetryPolicy.builder().maxRetry(2).fixedDelay(Duration.ofSeconds(10)).build())
                .onExhaustedStatus(TaskStatus.FAILED)
                .executor((TaskExecutor<Payload>) context -> TaskExecuteResult.success())
                .build();

        when(taskRunRepository.loadByRunId("run-1")).thenReturn(Optional.of(run));
        when(taskRepository.loadByTaskId("task-1")).thenReturn(Optional.of(task));
        when(taskPayloadRepository.loadByPayloadId("payload-1")).thenReturn(Optional.of(payloadPo));
        when(registry.findByPayloadClassAndTaskType(Payload.class, "EXPORT")).thenReturn(descriptor);

        TaskExecutionService service = new TaskExecutionService(taskRunRepository, taskRepository, taskPayloadRepository, registry);
        service.execute("run-1");

        ArgumentCaptor<TaskPo> taskCaptor = ArgumentCaptor.forClass(TaskPo.class);
        verify(taskRepository, atLeastOnce()).save(taskCaptor.capture());
        assertEquals(TaskStatus.SUCCESS, taskCaptor.getValue().getStatus());
        assertEquals(TaskRunStatus.SUCCESS, taskCaptor.getValue().getLastRunStatus());
    }

    @Test
    void shouldCreateRetryRunWhenExecutionFailsAndRetryBudgetRemains() {

        TaskRunPo run = new TaskRunPo();
        run.setRunId("run-1");
        run.setTaskId("task-1");
        run.setPayloadId("payload-1");
        run.setAttemptNo(1);

        TaskPo task = new TaskPo();
        task.setTaskId("task-1");
        task.setTaskType("EXPORT");
        task.setMode(TaskMode.ONCE);
        task.setRetryPolicyJson(Jsons.toJson(RetryPolicy.builder().maxRetry(2).fixedDelay(Duration.ofSeconds(10)).build()));
        task.setOnExhaustedStatus(TaskStatus.FAILED);

        TaskPayloadPo payloadPo = new TaskPayloadPo();
        payloadPo.setPayloadId("payload-1");
        payloadPo.setPayloadClass(Payload.class.getName());
        payloadPo.setPayloadJson(Jsons.toJson(new Payload("fail")));

        TaskExecutorDescriptor descriptor = TaskExecutorDescriptor.builder()
                .taskType("EXPORT")
                .payloadClass(Payload.class)
                .mode(TaskMode.ONCE)
                .retryPolicy(RetryPolicy.builder().maxRetry(2).fixedDelay(Duration.ofSeconds(10)).build())
                .onExhaustedStatus(TaskStatus.FAILED)
                .executor((TaskExecutor<Payload>) context -> TaskExecuteResult.failure("boom"))
                .build();

        when(taskRunRepository.loadByRunId("run-1")).thenReturn(Optional.of(run));
        when(taskRepository.loadByTaskId("task-1")).thenReturn(Optional.of(task));
        when(taskPayloadRepository.loadByPayloadId("payload-1")).thenReturn(Optional.of(payloadPo));
        when(registry.findByPayloadClassAndTaskType(Payload.class, "EXPORT")).thenReturn(descriptor);

        TaskExecutionService service = new TaskExecutionService(taskRunRepository, taskRepository, taskPayloadRepository, registry);
        service.execute("run-1");

        ArgumentCaptor<TaskRunPo> runCaptor = ArgumentCaptor.forClass(TaskRunPo.class);
        verify(taskRunRepository, atLeastOnce()).save(runCaptor.capture());
        assertEquals(TaskRunStatus.PENDING, runCaptor.getValue().getStatus());
        assertEquals(2, runCaptor.getValue().getAttemptNo());
    }

    @Test
    void shouldMoveReliableTaskToManualRequiredWhenRetryBudgetIsExhausted() {

        TaskRunPo run = new TaskRunPo();
        run.setRunId("run-1");
        run.setTaskId("task-1");
        run.setPayloadId("payload-1");
        run.setAttemptNo(11);

        TaskPo task = new TaskPo();
        task.setTaskId("task-1");
        task.setTaskType("NOTIFY");
        task.setMode(TaskMode.ONCE);
        task.setRetryPolicyJson(Jsons.toJson(RetryPolicy.builder().maxRetry(10).initialDelay(Duration.ofSeconds(2)).exponentialBackoff(true).build()));
        task.setOnExhaustedStatus(TaskStatus.MANUAL_REQUIRED);

        TaskPayloadPo payloadPo = new TaskPayloadPo();
        payloadPo.setPayloadId("payload-1");
        payloadPo.setPayloadClass(Payload.class.getName());
        payloadPo.setPayloadJson(Jsons.toJson(new Payload("fail")));

        TaskExecutorDescriptor descriptor = TaskExecutorDescriptor.builder()
                .taskType("NOTIFY")
                .payloadClass(Payload.class)
                .mode(TaskMode.ONCE)
                .retryPolicy(RetryPolicy.builder().maxRetry(10).initialDelay(Duration.ofSeconds(2)).exponentialBackoff(true).build())
                .onExhaustedStatus(TaskStatus.MANUAL_REQUIRED)
                .executor((TaskExecutor<Payload>) context -> TaskExecuteResult.failure("boom"))
                .build();

        when(taskRunRepository.loadByRunId("run-1")).thenReturn(Optional.of(run));
        when(taskRepository.loadByTaskId("task-1")).thenReturn(Optional.of(task));
        when(taskPayloadRepository.loadByPayloadId("payload-1")).thenReturn(Optional.of(payloadPo));
        when(registry.findByPayloadClassAndTaskType(Payload.class, "NOTIFY")).thenReturn(descriptor);

        TaskExecutionService service = new TaskExecutionService(taskRunRepository, taskRepository, taskPayloadRepository, registry);
        service.execute("run-1");

        ArgumentCaptor<TaskPo> taskCaptor = ArgumentCaptor.forClass(TaskPo.class);
        verify(taskRepository, atLeastOnce()).save(taskCaptor.capture());
        assertEquals(TaskStatus.MANUAL_REQUIRED, taskCaptor.getValue().getStatus());
    }

    record Payload(String name) {
    }

}
