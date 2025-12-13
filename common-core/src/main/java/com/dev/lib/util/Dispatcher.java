package com.dev.lib.util;

import java.util.concurrent.*;

public final class Dispatcher {

    private Dispatcher() {

    }

    /**
     * IO 密集型任务：数据库查询、RPC、文件读写
     */
    public static final ThreadPoolExecutor IO = new ThreadPoolExecutor(
            8,  // 核心线程为0，按需创建
            Math.max(64, Runtime.getRuntime().availableProcessors()),
            1L, TimeUnit.MINUTES,
            new SynchronousQueue<>(),  // 不排队，直接创建线程或拒绝
//            new ThreadFactoryBuilder().setNameFormat("io-%d").setDaemon(true).build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );


    /**
     * CPU 密集型任务：计算、序列化
     */
    public static final Executor DEFAULT = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

}
