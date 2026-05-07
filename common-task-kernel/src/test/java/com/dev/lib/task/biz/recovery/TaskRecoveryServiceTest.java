package com.dev.lib.task.biz.recovery;

import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.infra.data.TaskPayloadRepository;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import com.dev.lib.task.domain.RetryPolicy;
import com.dev.lib.util.Jsons;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskRecoveryServiceTest {

    @Mock
    private TaskRunRepository taskRunRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskPayloadRepository taskPayloadRepository;

    @Test
    void shouldCreateRecoveryRetryRunForInterruptedOnceTask() {

        TaskRunPo run = new TaskRunPo();
        run.setRunId("run-1");
        run.setTaskId("task-1");
        run.setPayloadId("payload-1");
        run.setAttemptNo(1);
        run.setStatus(TaskRunStatus.RUNNING);

        TaskPo task = new TaskPo();
        task.setTaskId("task-1");
        task.setMode(TaskMode.ONCE);
        task.setRetryPolicyJson(Jsons.toJson(RetryPolicy.builder().maxRetry(2).fixedDelay(Duration.ofSeconds(10)).build()));
        task.setOnExhaustedStatus(TaskStatus.FAILED);

        when(taskRunRepository.loadByRunId("run-1")).thenReturn(Optional.of(run));
        when(taskRepository.loadByTaskId("task-1")).thenReturn(Optional.of(task));

        TaskRecoveryService service = new TaskRecoveryService(taskRunRepository, taskRepository, taskPayloadRepository);
        service.recover("run-1");

        ArgumentCaptor<TaskRunPo> runCaptor = ArgumentCaptor.forClass(TaskRunPo.class);
        verify(taskRunRepository, atLeastOnce()).save(runCaptor.capture());
        assertEquals(TaskRunStatus.PENDING, runCaptor.getValue().getStatus());
        assertEquals(2, runCaptor.getValue().getAttemptNo());
    }
}
