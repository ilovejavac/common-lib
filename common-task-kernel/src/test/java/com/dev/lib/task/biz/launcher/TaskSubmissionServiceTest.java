package com.dev.lib.task.biz.launcher;

import com.dev.lib.task.biz.registry.TaskExecutorDescriptor;
import com.dev.lib.task.biz.registry.TaskExecutorRegistry;
import com.dev.lib.task.domain.RetryPolicy;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.infra.data.TaskPayloadPo;
import com.dev.lib.task.infra.data.TaskPayloadRepository;
import com.dev.lib.task.infra.data.TaskPo;
import com.dev.lib.task.infra.data.TaskRepository;
import com.dev.lib.task.infra.data.TaskRunPo;
import com.dev.lib.task.infra.data.TaskRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSubmissionServiceTest {

    @Mock
    private TaskExecutorRegistry registry;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskPayloadRepository taskPayloadRepository;

    @Mock
    private TaskRunRepository taskRunRepository;

    @Test
    void shouldCreateOnceTaskAndInitialRun() {

        TaskExecutorDescriptor descriptor = TaskExecutorDescriptor.builder()
                .application("report-service")
                .taskType("EXPORT_EXCEL")
                .payloadClass(ExportPayload.class)
                .mode(TaskMode.ONCE)
                .retryPolicy(RetryPolicy.builder().maxRetry(2).fixedDelay(Duration.ofSeconds(10)).build())
                .onExhaustedStatus(TaskStatus.FAILED)
                .build();
        when(registry.findByPayloadClass(ExportPayload.class)).thenReturn(List.of(descriptor));

        TaskSubmissionService service = new TaskSubmissionService(registry, taskRepository, taskPayloadRepository, taskRunRepository);
        String taskId = service.submit(null, null, new ExportPayload("finance"));

        ArgumentCaptor<TaskPo> taskCaptor = ArgumentCaptor.forClass(TaskPo.class);
        ArgumentCaptor<TaskPayloadPo> payloadCaptor = ArgumentCaptor.forClass(TaskPayloadPo.class);
        ArgumentCaptor<TaskRunPo> runCaptor = ArgumentCaptor.forClass(TaskRunPo.class);
        verify(taskRepository).save(taskCaptor.capture());
        verify(taskPayloadRepository).save(payloadCaptor.capture());
        verify(taskRunRepository).save(runCaptor.capture());

        assertNotNull(taskId);
        assertEquals(TaskStatus.PENDING, taskCaptor.getValue().getStatus());
        assertEquals("EXPORT_EXCEL", taskCaptor.getValue().getTaskType());
        assertEquals(TaskMode.ONCE, taskCaptor.getValue().getMode());
        assertEquals("finance", payloadCaptor.getValue().getPayloadJson().contains("finance") ? "finance" : null);
        assertEquals(1, runCaptor.getValue().getAttemptNo());
    }

    @Test
    void shouldCreateScheduleTaskWithoutInitialRun() {

        TaskExecutorDescriptor descriptor = TaskExecutorDescriptor.builder()
                .application("report-service")
                .taskType("DAILY_REPORT")
                .payloadClass(ReportPayload.class)
                .mode(TaskMode.SCHEDULE)
                .cron("0 0 2 * * ?")
                .retryPolicy(RetryPolicy.builder().maxRetry(10).initialDelay(Duration.ofSeconds(2)).exponentialBackoff(true).build())
                .onExhaustedStatus(TaskStatus.MANUAL_REQUIRED)
                .build();
        when(registry.findByPayloadClass(ReportPayload.class)).thenReturn(List.of(descriptor));

        TaskSubmissionService service = new TaskSubmissionService(registry, taskRepository, taskPayloadRepository, taskRunRepository);
        service.submit(null, null, new ReportPayload("daily"));

        ArgumentCaptor<TaskPo> taskCaptor = ArgumentCaptor.forClass(TaskPo.class);
        verify(taskRepository).save(taskCaptor.capture());
        verify(taskRunRepository, never()).save(any());
        assertEquals(TaskStatus.ENABLED, taskCaptor.getValue().getStatus());
        assertEquals(TaskMode.SCHEDULE, taskCaptor.getValue().getMode());
        assertNotNull(taskCaptor.getValue().getNextRunAt());
    }

    @Test
    void shouldRejectAmbiguousPayloadSubmissionWithoutTaskType() {

        TaskExecutorDescriptor first = TaskExecutorDescriptor.builder()
                .application("report-service")
                .taskType("EXPORT_A")
                .payloadClass(ExportPayload.class)
                .mode(TaskMode.ONCE)
                .retryPolicy(RetryPolicy.builder().maxRetry(2).fixedDelay(Duration.ofSeconds(10)).build())
                .onExhaustedStatus(TaskStatus.FAILED)
                .build();
        TaskExecutorDescriptor second = TaskExecutorDescriptor.builder()
                .application("report-service")
                .taskType("EXPORT_B")
                .payloadClass(ExportPayload.class)
                .mode(TaskMode.ONCE)
                .retryPolicy(RetryPolicy.builder().maxRetry(2).fixedDelay(Duration.ofSeconds(10)).build())
                .onExhaustedStatus(TaskStatus.FAILED)
                .build();
        when(registry.findByPayloadClass(ExportPayload.class)).thenReturn(List.of(first, second));

        TaskSubmissionService service = new TaskSubmissionService(registry, taskRepository, taskPayloadRepository, taskRunRepository);

        assertThrows(IllegalArgumentException.class, () -> service.submit(null, null, new ExportPayload("dup")));
    }

    @Test
    void shouldRejectRunAtForScheduleTask() {

        TaskExecutorDescriptor descriptor = TaskExecutorDescriptor.builder()
                .application("report-service")
                .taskType("DAILY_REPORT")
                .payloadClass(ReportPayload.class)
                .mode(TaskMode.SCHEDULE)
                .cron("0 0 2 * * ?")
                .retryPolicy(RetryPolicy.builder().maxRetry(10).initialDelay(Duration.ofSeconds(2)).exponentialBackoff(true).build())
                .onExhaustedStatus(TaskStatus.MANUAL_REQUIRED)
                .build();
        when(registry.findByPayloadClass(ReportPayload.class)).thenReturn(List.of(descriptor));

        TaskSubmissionService service = new TaskSubmissionService(registry, taskRepository, taskPayloadRepository, taskRunRepository);

        assertThrows(IllegalArgumentException.class,
                () -> service.submit(LocalDateTime.now().plusHours(1), null, new ReportPayload("daily")));
    }

    record ExportPayload(String scope) {
    }

    record ReportPayload(String name) {
    }

}
