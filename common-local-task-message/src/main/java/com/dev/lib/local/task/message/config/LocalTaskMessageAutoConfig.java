package com.dev.lib.local.task.message.config;

import com.dev.lib.local.task.message.domain.adapter.poller.LocalTaskPollerStorage;
import com.dev.lib.local.task.message.poller.core.DurationParser;
import com.dev.lib.local.task.message.poller.core.PollerEngine;
import com.dev.lib.local.task.message.poller.core.PollerEngineBuilder;
import com.dev.lib.local.task.message.poller.core.PollerEngineRegistry;
import com.dev.lib.local.task.message.poller.core.PollerTaskExecutor;
import com.dev.lib.local.task.message.poller.core.TaskType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * 本地任务消息自动配置
 * 仅支持注解配置方式 (@TaskType)
 */
@ComponentScan
@Configuration
@RequiredArgsConstructor
public class LocalTaskMessageAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(LocalTaskMessageAutoConfig.class);

    private final PollerEngineRegistry registry;
    private final LocalTaskPollerStorage storage;
    private final List<PollerTaskExecutor> executors;

    @PostConstruct
    public void registerEngines() {
        for (PollerTaskExecutor executor : executors) {
            TaskType annotation = executor.getClass().getAnnotation(TaskType.class);
            if (annotation == null) {
                log.warn("PollerTaskExecutor [{}] missing @TaskType annotation, skipping",
                        executor.getClass().getSimpleName());
                continue;
            }

            String taskType = annotation.value();

            if (!annotation.enabled()) {
                log.info("PollerTaskExecutor disabled for taskType: {}, skipping", taskType);
                continue;
            }

            Duration pollInterval = DurationParser.parseOrDefault(
                    annotation.pollInterval(), Duration.ofSeconds(10));
            Duration baseDelay = DurationParser.parseOrDefault(
                    annotation.baseDelay(), Duration.ofSeconds(1));
            Duration maxDelay = DurationParser.parseOrDefault(
                    annotation.maxDelay(), Duration.ofMinutes(5));

            PollerEngine engine = PollerEngineBuilder.builder()
                    .taskType(taskType)
                    .houseNumbers(annotation.houseNumberCount())
                    .pollInterval(pollInterval)
                    .fetchLimit(annotation.fetchLimit())
                    .maxRetry(annotation.maxRetry())
                    .baseDelay(baseDelay)
                    .maxDelay(maxDelay)
                    .backoffStrategy(annotation.backoffStrategy())
                    .timeoutMinutes(annotation.timeoutMinutes())
                    .executor(executor)
                    .storage(storage)
                    .build();

            registry.register(engine);
            log.info("Auto-registered PollerEngine: taskType={}, houseNumbers={}, pollInterval={}, maxRetry={}",
                    taskType, annotation.houseNumberCount(), pollInterval, annotation.maxRetry());
        }
    }
}
