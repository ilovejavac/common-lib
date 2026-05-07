package com.dev.lib.task.biz.registry;

import com.dev.lib.task.annotation.Task;
import com.dev.lib.task.domain.AsyncTaskExecutor;
import com.dev.lib.task.domain.ReliableTaskExecutor;
import com.dev.lib.task.domain.ScheduleTaskExecutor;
import com.dev.lib.task.domain.TaskExecuteContext;
import com.dev.lib.task.domain.TaskExecuteResult;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskExecutorRegistryTest {

    @Test
    void shouldUseClassNameAsDefaultTaskType() {

        TaskExecutorRegistry registry = new TaskExecutorRegistry(List.of(new ExportExecutor()), "report-service");
        registry.init();

        List<TaskExecutorDescriptor> descriptors = registry.findByPayloadClass(ExportPayload.class);

        assertEquals(1, descriptors.size());
        assertEquals("ExportExecutor", descriptors.get(0).getTaskType());
        assertEquals(TaskMode.ONCE, descriptors.get(0).getMode());
        assertEquals(TaskStatus.FAILED, descriptors.get(0).getOnExhaustedStatus());
    }

    @Test
    void shouldUseAnnotationValueWhenProvidedAndInferReliableDefaults() {

        TaskExecutorRegistry registry = new TaskExecutorRegistry(List.of(new NotifyExecutor()), "notify-service");
        registry.init();

        TaskExecutorDescriptor descriptor = registry.findByPayloadClassAndTaskType(NotifyPayload.class, "ORDER_NOTIFY");

        assertEquals("ORDER_NOTIFY", descriptor.getTaskType());
        assertEquals(10, descriptor.getRetryPolicy().getMaxRetry());
        assertEquals(TaskStatus.MANUAL_REQUIRED, descriptor.getOnExhaustedStatus());
    }

    @Test
    void shouldInferScheduleModeFromExecutorTypeAndRequireCron() {

        TaskExecutorRegistry registry = new TaskExecutorRegistry(List.of(new DailyReportExecutor()), "report-service");
        registry.init();

        TaskExecutorDescriptor descriptor = registry.findByPayloadClassAndTaskType(ReportPayload.class, "DAILY_REPORT");

        assertEquals(TaskMode.SCHEDULE, descriptor.getMode());
        assertEquals("0 0 2 * * ?", descriptor.getCron());
    }

    @Test
    void shouldRejectDuplicatePayloadAndTaskTypeRegistration() {

        TaskExecutorRegistry registry = new TaskExecutorRegistry(
                List.of(new DuplicateExecutorA(), new DuplicateExecutorB()),
                "report-service"
        );

        assertThrows(IllegalStateException.class, registry::init);
    }

    static class ExportPayload {
    }

    static class NotifyPayload {
    }

    static class ReportPayload {
    }

    static class DuplicatePayload {
    }

    @Task
    static class ExportExecutor implements AsyncTaskExecutor<ExportPayload> {

        @Override
        public TaskExecuteResult execute(TaskExecuteContext<ExportPayload> context) {

            return TaskExecuteResult.success();
        }
    }

    @Task("ORDER_NOTIFY")
    static class NotifyExecutor implements ReliableTaskExecutor<NotifyPayload> {

        @Override
        public TaskExecuteResult execute(TaskExecuteContext<NotifyPayload> context) {

            return TaskExecuteResult.success();
        }
    }

    @Task(value = "DAILY_REPORT", cron = "0 0 2 * * ?")
    static class DailyReportExecutor implements ScheduleTaskExecutor<ReportPayload> {

        @Override
        public TaskExecuteResult execute(TaskExecuteContext<ReportPayload> context) {

            return TaskExecuteResult.success();
        }
    }

    @Task("DUPLICATE")
    static class DuplicateExecutorA implements AsyncTaskExecutor<DuplicatePayload> {

        @Override
        public TaskExecuteResult execute(TaskExecuteContext<DuplicatePayload> context) {

            return TaskExecuteResult.success();
        }
    }

    @Task("DUPLICATE")
    static class DuplicateExecutorB implements AsyncTaskExecutor<DuplicatePayload> {

        @Override
        public TaskExecuteResult execute(TaskExecuteContext<DuplicatePayload> context) {

            return TaskExecuteResult.success();
        }
    }

}
