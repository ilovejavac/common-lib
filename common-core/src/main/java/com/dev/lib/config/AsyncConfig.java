package com.dev.lib.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executor;

@Component
public class AsyncConfig implements AsyncConfigurer {

    private final static TaskExecutor EXECUTOR = taskExecutor();

    private static TaskExecutor taskExecutor() {

        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("async-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(600);
        executor.setTaskDecorator(new MdcTaskDecorator());
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {

        return EXECUTOR;
    }

    static class MdcTaskDecorator implements TaskDecorator {

        @Override
        public @NonNull Runnable decorate(@NonNull Runnable runnable) {

            Map<String, String> parent = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> old = MDC.getCopyOfContextMap();
                try {
                    if (parent != null) {
                        MDC.setContextMap(parent);
                    } else {
                        MDC.clear();
                    }
                    runnable.run();
                } finally {
                    if (old != null) {
                        MDC.setContextMap(old);
                    } else {
                        MDC.clear();
                    }
                }
            };
        }

    }

}

