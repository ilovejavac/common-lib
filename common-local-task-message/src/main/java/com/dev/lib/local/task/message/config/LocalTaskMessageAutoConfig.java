package com.dev.lib.local.task.message.config;

import com.dev.lib.local.task.message.domain.adapter.poller.LocalTaskPollerStorage;
import com.dev.lib.local.task.message.poller.core.PollerEngine;
import com.dev.lib.local.task.message.poller.core.PollerEngineBuilder;
import com.dev.lib.local.task.message.poller.core.PollerEngineRegistry;
import com.dev.lib.local.task.message.poller.core.PollerTaskExecutor;
import com.dev.lib.local.task.message.poller.core.TaskType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ComponentScan
@Configuration
@RequiredArgsConstructor
public class LocalTaskMessageAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(LocalTaskMessageAutoConfig.class);

    private final PollerEngineRegistry registry;
    private final LocalTaskPollerStorage storage;
    private final List<PollerTaskExecutor> executors;

    @Bean
    @ConfigurationProperties(prefix = "app.local-task-message")
    public LocalTaskConfigProperties localTaskConfigProperties() {
        return new LocalTaskConfigProperties();
    }

    /**
     * 自动注册 PollerEngine
     * 根据配置文件自动创建并注册各个任务类型的引擎
     */
    @Bean
    @ConditionalOnBean(PollerTaskExecutor.class)
    public PollerEngineAutoRegistrar pollerEngineAutoRegistrar(LocalTaskConfigProperties properties) {
        return new PollerEngineAutoRegistrar(registry, storage, executors, properties);
    }

    /**
     * PollerEngine 自动注册器
     */
    public static class PollerEngineAutoRegistrar {

        private static final Logger log = LoggerFactory.getLogger(PollerEngineAutoRegistrar.class);

        private final PollerEngineRegistry registry;
        private final LocalTaskPollerStorage storage;
        private final List<PollerTaskExecutor> executors;
        private final LocalTaskConfigProperties properties;

        public PollerEngineAutoRegistrar(
                PollerEngineRegistry registry,
                LocalTaskPollerStorage storage,
                List<PollerTaskExecutor> executors,
                LocalTaskConfigProperties properties) {
            this.registry = registry;
            this.storage = storage;
            this.executors = executors;
            this.properties = properties;
            registerEngines();
        }

        private void registerEngines() {
            // 如果配置了 typeConfigs，按配置创建引擎
            if (properties.getTypeConfigs() != null && !properties.getTypeConfigs().isEmpty()) {
                for (LocalTaskConfigProperties.TaskTypeConfig typeConfig : properties.getTypeConfigs()) {
                    if (!typeConfig.isEnabled()) {
                        continue;
                    }

                    // 查找对应的 executor
                    PollerTaskExecutor executor = findExecutor(typeConfig.getTaskType());
                    if (executor == null) {
                        log.warn("No PollerTaskExecutor found for taskType: {}, skipping", typeConfig.getTaskType());
                        continue;
                    }

                    // 创建并注册引擎
                    PollerEngine engine = PollerEngineBuilder.builder()
                            .taskType(typeConfig.getTaskType())
                            .houseNumbers(typeConfig.getHouseNumberCount())
                            .pollInterval(typeConfig.getPollInterval())
                            .fetchLimit(typeConfig.getFetchLimit())
                            .maxRetry(typeConfig.getMaxRetry())
                            .baseDelay(typeConfig.getBaseDelay())
                            .maxDelay(typeConfig.getMaxDelay())
                            .backoffStrategy(typeConfig.getBackoffStrategy())
                            .executor(executor)
                            .storage(storage)
                            .build();

                    registry.register(engine);
                    log.info("Auto-registered PollerEngine for taskType: {}", typeConfig.getTaskType());
                }
            } else {
                // 如果没有配置，尝试自动注册所有带 @TaskType 注解的 executor
                autoRegisterAllExecutors();
            }
        }

        private void autoRegisterAllExecutors() {
            for (PollerTaskExecutor executor : executors) {
                TaskType annotation = executor.getClass().getAnnotation(TaskType.class);
                if (annotation != null) {
                    String taskType = annotation.value();
                    PollerEngine engine = PollerEngineBuilder.builder()
                            .taskType(taskType)
                            .houseNumbers(5)  // 默认 5 个门牌号
                            .pollInterval(properties.getGlobal().getPollInterval())
                            .fetchLimit(128)
                            .maxRetry(properties.getGlobal().getMaxRetry())
                            .baseDelay(properties.getGlobal().getBaseDelay())
                            .maxDelay(properties.getGlobal().getMaxDelay())
                            .backoffStrategy(properties.getGlobal().getBackoffStrategy())
                            .executor(executor)
                            .storage(storage)
                            .build();

                    registry.register(engine);
                    log.info("Auto-registered PollerEngine for taskType: {} (default config)", taskType);
                }
            }
        }

        private PollerTaskExecutor findExecutor(String taskType) {
            // 首先尝试通过 @TaskType 注解查找
            for (PollerTaskExecutor executor : executors) {
                TaskType annotation = executor.getClass().getAnnotation(TaskType.class);
                if (annotation != null && annotation.value().equals(taskType)) {
                    return executor;
                }
            }

            // 如果没找到，通过类名模糊匹配
            return executors.stream()
                    .filter(e -> {
                        String className = e.getClass().getSimpleName();
                        String expectedName = taskType.replace("_", "") + "PollerTaskExecutor";
                        return className.equalsIgnoreCase(expectedName);
                    })
                    .findFirst()
                    .orElse(null);
        }
    }

}
