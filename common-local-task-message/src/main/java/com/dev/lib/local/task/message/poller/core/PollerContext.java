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
     * 任务名称（可选，用于描述）
     */
    private String taskName;

    /**
     * 任务超时时间（分钟）
     */
    private Integer timeoutMinutes;

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

    public PollerContext(String id, String taskType, String taskName, Map<String, Object> payload, int retryCount, String errorMessage) {
        this.id = id;
        this.taskType = taskType;
        this.taskName = taskName;
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

    public String getTaskName() {
        return taskName;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
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

    // 手动添加 setter 方法
    public void setId(String id) {
        this.id = id;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

}
