package com.dev.lib.local.task.message.poller.core;

/**
 * 轮询任务执行器接口
 * 业务模块需实现此接口来处理具体的任务执行逻辑
 */
public interface PollerTaskExecutor {

    /**
     * 执行任务
     *
     * @param context 任务上下文
     * @return 执行结果，明确表示成功或失败
     */
    PollerResult execute(PollerContext context);

    /**
     * 任务成功回调
     * 默认空实现，业务方可选择重写
     *
     * @param context 任务上下文
     */
    default void onSuccess(PollerContext context) {
        // 默认空实现
    }

    /**
     * 任务失败回调
     * 默认空实现，业务方可选择重写
     *
     * @param context      任务上下文
     * @param errorMessage 错误信息
     */
    default void onFailure(PollerContext context, String errorMessage) {
        // 默认空实现
    }

}
