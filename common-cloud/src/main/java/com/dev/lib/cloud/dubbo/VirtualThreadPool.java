package com.dev.lib.cloud.dubbo;

import com.dev.lib.util.limiter.ConcurrencyLimiter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.*;

public class VirtualThreadPool implements ThreadPool {

    private final ConcurrencyLimiter limiter = ConcurrencyLimiter.builder()
            .maxConcurrency(1000)
            .build();

    @Override
    public Executor getExecutor(URL url) {

        return new VirtualThreadExecutorService();
    }

    private class VirtualThreadExecutorService extends AbstractExecutorService {

        private final    ExecutorService virtual  = Executors.newVirtualThreadPerTaskExecutor();

        private volatile boolean         shutdown = false;

        @Override
        public void execute(@NonNull Runnable command) {

            try {
                limiter.executeVoid(() -> virtual.execute(command));
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void shutdown() {

            shutdown = true;
            virtual.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {

            shutdown = true;
            return virtual.shutdownNow();
        }

        @Override
        public boolean isShutdown() {

            return shutdown;
        }

        @Override
        public boolean isTerminated() {

            return virtual.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {

            return virtual.awaitTermination(timeout, unit);
        }

    }

}