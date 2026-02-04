package com.dev.lib.local.task.message.poller.core;

/**
 * 任务执行结果
 * 用于明确表示任务执行的成功或失败状态
 */
public class PollerResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果数据（成功时）
     */
    private Object data;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 是否需要重试（失败时）
     * 默认为 true，表示失败后需要重试
     */
    private boolean retryable = true;

    public PollerResult() {
    }

    public PollerResult(boolean success, Object data, String errorMessage, boolean retryable) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
    }

    /**
     * 创建成功结果
     */
    public static PollerResult success() {
        return new PollerResult(true, null, null, false);
    }

    /**
     * 创建成功结果（带数据）
     */
    public static PollerResult success(Object data) {
        return new PollerResult(true, data, null, false);
    }

    /**
     * 创建失败结果（可重试）
     */
    public static PollerResult failure(String errorMessage) {
        return new PollerResult(false, null, errorMessage, true);
    }

    /**
     * 创建失败结果（不可重试，直接标记失败）
     */
    public static PollerResult failureNoRetry(String errorMessage) {
        return new PollerResult(false, null, errorMessage, false);
    }

    /**
     * 创建需要重试的结果
     */
    public static PollerResult retry(String errorMessage) {
        return new PollerResult(false, null, errorMessage, true);
    }

    // 手动添加 getter 方法
    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }

}
