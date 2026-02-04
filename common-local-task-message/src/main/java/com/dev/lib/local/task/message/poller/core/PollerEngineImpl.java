package com.dev.lib.local.task.message.poller.core;

import com.dev.lib.entity.id.IDWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Poller 轮询引擎实现
 * 每个任务类型使用独立的虚拟线程池进行并发处理
 */
public class PollerEngineImpl implements PollerEngine {

    private static final Logger log = LoggerFactory.getLogger(PollerEngineImpl.class);

    private final PollerConfig config;

    private final PollerStorage storage;

    private final PollerTaskExecutor executor;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler;

    private ExecutorService virtualExecutor;

    public PollerEngineImpl(PollerConfig config, PollerStorage storage, PollerTaskExecutor executor) {
        this.config = config;
        this.storage = storage;
        this.executor = executor;
    }

    @Override
    public String getTaskType() {

        return config.getTaskType();
    }

    @Override
    public String submit(String businessId, Map<String, Object> payload) {

        return submit(IDWorker.newId(), businessId, payload);
    }

    @Override
    public String submit(String taskId, String businessId, Map<String, Object> payload) {
        // 计算门牌号
        int houseNumber = calculateHouseNumber(businessId);

        // 构建任务上下文
        PollerContext context = new PollerContext(
                taskId,
                config.getTaskType(),
                payload,
                0,
                null
        );

        // 持久化任务
        storage.save(context, houseNumber);

        log.debug(
                "Task submitted: taskId={}, taskType={}, businessId={}, houseNumber={}",
                taskId, config.getTaskType(), businessId, houseNumber
        );

        return taskId;
    }

    @Override
    public void start() {

        if (running.compareAndSet(false, true)) {
            log.info("Starting PollerEngine for taskType: {}", config.getTaskType());

            // 创建虚拟线程执行器（每个任务类型独立的虚拟线程池）
            virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

            // 创建调度器（使用虚拟线程）
            scheduler = Executors.newSingleThreadScheduledExecutor(
                    Thread.ofVirtual().factory()
            );

            // 启动调度任务
            scheduler.scheduleWithFixedDelay(
                    this::poll,
                    0,
                    config.getPollInterval().toMillis(),
                    TimeUnit.MILLISECONDS
            );

            log.info(
                    "PollerEngine started for taskType: {} with {} houseNumbers (virtual threads)",
                    config.getTaskType(), config.getHouseNumbers().size()
            );
        }
    }

    @Override
    public void stop() {

        if (running.compareAndSet(true, false)) {
            log.info("Stopping PollerEngine for taskType: {}", config.getTaskType());

            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            if (virtualExecutor != null) {
                virtualExecutor.shutdown();
                try {
                    if (!virtualExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        virtualExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    virtualExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            log.info("PollerEngine stopped for taskType: {}", config.getTaskType());
        }
    }

    @Override
    public boolean isRunning() {

        return running.get();
    }

    /**
     * 轮询并处理任务
     */
    private void poll() {

        if (!running.get()) {
            return;
        }

        try {
            // 获取待处理任务
            List<PollerContext> tasks = storage.fetchPending(
                    config.getTaskType(),
                    config.getHouseNumbers(),
                    null,
                    config.getFetchLimit()
            );

            if (!tasks.isEmpty()) {
                log.debug(
                        "Fetched {} pending tasks for taskType: {}",
                        tasks.size(), config.getTaskType()
                );

                // 并发处理任务（每个任务在独立的虚拟线程中执行）
                for (PollerContext task : tasks) {
                    virtualExecutor.submit(() -> processTask(task));
                }
            }
        } catch (Exception e) {
            log.error("Error polling tasks for taskType: {}", config.getTaskType(), e);
        }
    }

    /**
     * 处理单个任务
     */
    private void processTask(PollerContext task) {

        String taskId = task.getId();

        try {
            // CAS 更新状态为处理中
            if (!storage.updateToProcessing(taskId)) {
                log.debug("Task {} already being processed by another thread", taskId);
                return;
            }

            log.debug(
                    "Processing task: taskId={}, taskType={}, retryCount={}",
                    taskId, task.getTaskType(), task.getRetryCount()
            );

            // 执行任务
            PollerResult result = executor.execute(task);

            // 根据执行结果更新状态
            if (result.isSuccess()) {
                storage.updateToSuccess(taskId);
                log.info(
                        "Task completed successfully: taskId={}, taskType={}",
                        taskId, task.getTaskType()
                );
            } else {
                // 执行失败
                if (!result.isRetryable() || task.getRetryCount() + 1 >= config.getMaxRetry()) {
                    // 不可重试或达到最大重试次数
                    storage.updateToFailed(task.getId(), result.getErrorMessage(), null);
                    log.warn("Task failed (no retry): taskId={}, error={}", taskId, result.getErrorMessage());
                } else {
                    // 可重试
                    handleRetry(task, result.getErrorMessage());
                }
            }

        } catch (Exception e) {
            log.error(
                    "Task execution failed: taskId={}, taskType={}, retryCount={}",
                    taskId, task.getTaskType(), task.getRetryCount(), e
            );

            handleFailure(task, e);
        }
    }

    /**
     * 处理任务重试
     */
    private void handleRetry(PollerContext task, String errorMessage) {

        int newRetryCount = task.getRetryCount() + 1;

        // 计算下次重试时间
        long delayMs = config.getBackoffStrategy().calculateDelay(
                newRetryCount,
                config.getBaseDelay(),
                config.getMaxDelay()
        );
        LocalDateTime nextRetryTime = LocalDateTime.now().plus(Duration.ofMillis(delayMs));

        storage.updateToFailed(task.getId(), errorMessage, nextRetryTime);

        log.debug(
                "Task will be retried at {}: taskId={}, retry={}/{}",
                nextRetryTime, task.getId(), newRetryCount, config.getMaxRetry()
        );
    }

    /**
     * 处理任务失败（异常情况）
     */
    private void handleFailure(PollerContext task, Exception e) {

        int newRetryCount = task.getRetryCount() + 1;

        if (newRetryCount >= config.getMaxRetry()) {
            // 达到最大重试次数，不再重试
            storage.updateToFailed(task.getId(), "Max retry exceeded: " + e.getMessage(), null);
            log.warn("Task failed after {} retries: taskId={}", newRetryCount, task.getId());
        } else {
            // 计算下次重试时间
            long delayMs = config.getBackoffStrategy().calculateDelay(
                    newRetryCount,
                    config.getBaseDelay(),
                    config.getMaxDelay()
            );
            LocalDateTime nextRetryTime = LocalDateTime.now().plus(Duration.ofMillis(delayMs));

            storage.updateToFailed(task.getId(), e.getMessage(), nextRetryTime);

            log.debug(
                    "Task will be retried at {}: taskId={}, retry={}/{}",
                    nextRetryTime, task.getId(), newRetryCount, config.getMaxRetry()
            );
        }
    }

    /**
     * 计算门牌号
     * businessId % houseNumberCount
     */
    private int calculateHouseNumber(String businessId) {

        int houseNumberCount = config.getHouseNumbers().size();
        int index            = Math.toIntExact(IDWorker.nextID() % houseNumberCount);
        return config.getHouseNumbers().get(index);
    }

}
