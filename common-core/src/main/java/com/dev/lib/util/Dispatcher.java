package com.dev.lib.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class Dispatcher {

    private Dispatcher() {

    }

    /**
     * IO 密集型任务：数据库查询、RPC、文件读写
     */
    public static final Executor IO = Executors.newFixedThreadPool(
            Math.max(
                    64,
                    Runtime.getRuntime().availableProcessors()
            )
    );

    /**
     * CPU 密集型任务：计算、序列化
     */
    public static final Executor DEFAULT = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

}
