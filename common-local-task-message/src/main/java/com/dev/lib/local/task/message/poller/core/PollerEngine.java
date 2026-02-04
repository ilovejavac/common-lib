package com.dev.lib.local.task.message.poller.core;

/**
 * Poller 轮询引擎接口
 * 提供任务注册、提交、启动和停止功能
 */
public interface PollerEngine {

    /**
     * 获取任务类型
     *
     * @return 任务类型
     */
    String getTaskType();

    /**
     * 提交任务
     * 根据 businessId 自动计算门牌号并持久化
     *
     * @param businessId 业务ID（用于计算门牌号和幂等）
     * @param payload    任务数据
     * @return 任务ID
     */
    String submit(String businessId, java.util.Map<String, Object> payload);

    /**
     * 提交任务（指定任务ID）
     *
     * @param taskId     任务ID
     * @param businessId 业务ID
     * @param payload    任务数据
     * @return 任务ID
     */
    String submit(String taskId, String businessId, java.util.Map<String, Object> payload);

    /**
     * 启动轮询引擎
     */
    void start();

    /**
     * 停止轮询引擎
     */
    void stop();

    /**
     * 检查引擎是否正在运行
     *
     * @return 是否运行中
     */
    boolean isRunning();

}
