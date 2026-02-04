package com.dev.lib.local.task.message.poller.core;

import java.util.List;

/**
 * Poller 存储接口
 * 用于任务数据的持久化和查询
 */
public interface PollerStorage {

    /**
     * 保存任务
     *
     * @param task       任务上下文
     * @param houseNumber 门牌号（由 businessId % houseNumber 计算得出）
     */
    void save(PollerContext task, int houseNumber);

    /**
     * 获取待处理任务列表
     *
     * @param taskType    任务类型
     * @param houseNumbers 门牌号列表
     * @param lastId      上次查询的最后 ID（用于分页）
     * @param limit       限制数量
     * @return 待处理任务列表
     */
    List<PollerContext> fetchPending(String taskType, List<Integer> houseNumbers, Long lastId, int limit);

    /**
     * 更新任务状态为处理中（CAS 操作，防止重复执行）
     *
     * @param taskId 任务ID
     * @return 是否更新成功（false 表示已被其他线程更新）
     */
    boolean updateToProcessing(String taskId);

    /**
     * 更新任务状态为成功
     *
     * @param taskId 任务ID
     */
    void updateToSuccess(String taskId);

    /**
     * 更新任务状态为失败
     *
     * @param taskId       任务ID
     * @param errorMessage  错误信息
     * @param nextRetryTime 下次重试时间（null 表示不重试）
     */
    void updateToFailed(String taskId, String errorMessage, java.time.LocalDateTime nextRetryTime);

}
