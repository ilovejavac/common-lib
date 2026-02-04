package com.dev.lib.local.task.message.poller.core;

import java.util.Map;

/**
 * 轮询任务上下文
 */
public class PollerContext {

    /**
     * 任务ID
     */
    private String id;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 业务数据
     */
    private Map<String, Object> payload;

    /**
     * 当前重试次数
     */
    private int retryCount;

    /**
     * 最后一次错误信息
     */
    private String errorMessage;

    public PollerContext() {
    }

    public PollerContext(String id, String taskType, Map<String, Object> payload, int retryCount, String errorMessage) {
        this.id = id;
        this.taskType = taskType;
        this.payload = payload;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
    }

    // 手动添加 getter 方法
    public String getId() {
        return id;
    }

    public String getTaskType() {
        return taskType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
