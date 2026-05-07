package com.dev.lib.task.infra.data;

import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import com.dev.lib.task.domain.TriggerSource;
import com.dev.lib.entity.encrypt.EncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRunRepositoryConcurrencyIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TaskRunRepositoryConcurrencyTestApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:task_run_concurrency;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.jpa.show-sql=false",
                    "spring.application.name=task-run-concurrency-test"
            );

    @Test
    void shouldSkipLockedRunInConcurrentTransaction() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            TaskRepository taskRepository = context.getBean(TaskRepository.class);
            TaskPayloadRepository taskPayloadRepository = context.getBean(TaskPayloadRepository.class);
            TaskRunRepository taskRunRepository = context.getBean(TaskRunRepository.class);
            LockingProbeService lockingProbeService = context.getBean(LockingProbeService.class);

            seedRunnableTask(taskRepository, taskPayloadRepository, taskRunRepository);

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch locked = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            LocalDateTime now = LocalDateTime.now();
            AtomicReference<Throwable> firstFailure = new AtomicReference<>();

            try {
                Future<List<TaskRunPo>> first = executorService.submit(() -> {
                    try {
                        return lockingProbeService.lockAndHold(now, locked, release);
                    } catch (Throwable throwable) {
                        firstFailure.set(throwable);
                        locked.countDown();
                        throw throwable;
                    }
                });
                assertThat(locked.await(5, TimeUnit.SECONDS))
                        .withFailMessage(() -> "The first transaction never acquired a row lock. Root cause: " + firstFailure.get())
                        .isTrue();

                Future<List<TaskRunPo>> second = executorService.submit(() -> lockingProbeService.loadRunnable(now));

                List<TaskRunPo> secondResult = second.get(5, TimeUnit.SECONDS);
                assertThat(secondResult).isEmpty();

                release.countDown();

                List<TaskRunPo> firstResult = first.get(5, TimeUnit.SECONDS);
                assertThat(firstResult).hasSize(1);
                assertThat(firstResult.getFirst().getRunId()).isEqualTo("run-1");
            } catch (ExecutionException executionException) {
                throw new IllegalStateException("Concurrent row-lock test failed", executionException.getCause());
            } finally {
                release.countDown();
                executorService.shutdownNow();
            }
        });
    }

    private static void seedRunnableTask(TaskRepository taskRepository,
                                         TaskPayloadRepository taskPayloadRepository,
                                         TaskRunRepository taskRunRepository) {

        TaskPo task = new TaskPo();
        task.setTaskId("task-1");
        task.setApplication("task-run-concurrency-test");
        task.setTaskType("EXPORT_EXCEL");
        task.setMode(TaskMode.ONCE);
        task.setStatus(TaskStatus.PENDING);
        task.setPayloadId("payload-1");
        taskRepository.save(task);

        TaskPayloadPo payload = new TaskPayloadPo();
        payload.setPayloadId("payload-1");
        payload.setTaskId("task-1");
        payload.setPayloadClass("java.lang.String");
        payload.setPayloadJson("{\"scope\":\"finance\"}");
        taskPayloadRepository.save(payload);

        TaskRunPo run = new TaskRunPo();
        run.setRunId("run-1");
        run.setTaskId("task-1");
        run.setPayloadId("payload-1");
        run.setAttemptNo(1);
        run.setStatus(TaskRunStatus.PENDING);
        run.setTriggerSource(TriggerSource.SUBMIT);
        run.setScheduledAt(LocalDateTime.now().minusSeconds(1));
        taskRunRepository.save(run);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(LockingProbeService.class)
    static class TaskRunRepositoryConcurrencyTestApplication {

        @Bean
        EncryptionService encryptionService() {

            return new EncryptionService() {
                @Override
                public String encrypt(String dbValue) {

                    return dbValue;
                }

                @Override
                public String decrypt(String dbValue) {

                    return dbValue;
                }
            };
        }
    }

    static class LockingProbeService {

        private final TaskRunRepository taskRunRepository;

        LockingProbeService(TaskRunRepository taskRunRepository) {

            this.taskRunRepository = taskRunRepository;
        }

        @Transactional
        public List<TaskRunPo> lockAndHold(LocalDateTime now, CountDownLatch locked, CountDownLatch release)
                throws InterruptedException {

            List<TaskRunPo> runs = taskRunRepository.loadsRunnableForUpdate(now, 1);
            locked.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out while waiting to release row lock");
            }
            return runs;
        }

        @Transactional
        public List<TaskRunPo> loadRunnable(LocalDateTime now) {

            return taskRunRepository.loadsRunnableForUpdate(now, 1);
        }
    }

}
