package com.dev.lib.util;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Dispatcher implements DisposableBean {

    private Dispatcher() {

    }

    /**
     * IO密集：数据库、RPC、文件
     */
    public static final ExecutorService IO =
            Executors.newVirtualThreadPerTaskExecutor();

    /**
     * CPU密集：计算、序列化
     */
    public static final ExecutorService DEFAULT =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public void destroy() {

        IO.shutdown();
        DEFAULT.close();
    }

}
