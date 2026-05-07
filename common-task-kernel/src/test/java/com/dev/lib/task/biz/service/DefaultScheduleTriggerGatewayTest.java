package com.dev.lib.task.biz.service;

import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.domain.TriggerSource;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultScheduleTriggerGatewayTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskRunRepository taskRunRepository;

    @Test
    void shouldCreatePendingRunForEnabledScheduleTask() {

        TaskPo task = new TaskPo();
        task.setTaskId("task-1");
        task.setMode(TaskMode.SCHEDULE);
        task.setStatus(TaskStatus.ENABLED);
        task.setPayloadId("payload-1");
        when(taskRepository.loadByTaskId("task-1")).thenReturn(Optional.of(task));
        when(taskRunRepository.existsByTaskIdAndTriggerTime("task-1", LocalDateTime.of(2026, 5, 6, 2, 0)))
                .thenReturn(false);

        DefaultScheduleTriggerGateway gateway = new DefaultScheduleTriggerGateway(taskRepository, taskRunRepository);
        gateway.fire("task-1", LocalDateTime.of(2026, 5, 6, 2, 0), TriggerSource.SCHEDULE);

        ArgumentCaptor<TaskRunPo> runCaptor = ArgumentCaptor.forClass(TaskRunPo.class);
        verify(taskRunRepository).save(runCaptor.capture());
        assertEquals("task-1", runCaptor.getValue().getTaskId());
        assertEquals(TriggerSource.SCHEDULE, runCaptor.getValue().getTriggerSource());
    }

    @Test
    void shouldIgnoreDuplicateTriggerForSameScheduleTime() {

        TaskPo task = new TaskPo();
        task.setTaskId("task-1");
        task.setMode(TaskMode.SCHEDULE);
        task.setStatus(TaskStatus.ENABLED);
        when(taskRepository.loadByTaskId("task-1")).thenReturn(Optional.of(task));
        when(taskRunRepository.existsByTaskIdAndTriggerTime("task-1", LocalDateTime.of(2026, 5, 6, 2, 0)))
                .thenReturn(true);

        DefaultScheduleTriggerGateway gateway = new DefaultScheduleTriggerGateway(taskRepository, taskRunRepository);
        gateway.fire("task-1", LocalDateTime.of(2026, 5, 6, 2, 0), TriggerSource.SCHEDULE);

        verify(taskRunRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectNonScheduleTask() {

        TaskPo task = new TaskPo();
        task.setTaskId("task-1");
        task.setMode(TaskMode.ONCE);
        task.setStatus(TaskStatus.PENDING);
        when(taskRepository.loadByTaskId("task-1")).thenReturn(Optional.of(task));

        DefaultScheduleTriggerGateway gateway = new DefaultScheduleTriggerGateway(taskRepository, taskRunRepository);

        assertThrows(IllegalArgumentException.class,
                () -> gateway.fire("task-1", LocalDateTime.of(2026, 5, 6, 2, 0), TriggerSource.SCHEDULE));
    }
}
