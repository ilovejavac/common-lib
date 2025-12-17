package com.dev.lib.dubbo;

import com.dev.lib.util.limiter.ConcurrencyLimiter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VirtualThreadPool implements ThreadPool {

    private final ConcurrencyLimiter limiter = ConcurrencyLimiter.builder()
            .maxConcurrency(1000)
            .build();

    private final Executor virtual = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public Executor getExecutor(URL url) {

        return runnable -> {
            try {
                limiter.executeVoid(() -> virtual.execute(runnable));
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        };
    }

}
