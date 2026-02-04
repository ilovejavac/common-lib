package com.dev.lib.local.task.message.data;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 本地任务消息持久化实体
 * 简化设计：只保留核心字段，通过 taskType 区分不同任务类型
 */
@Data
@Entity
@DynamicUpdate
@Table(name = "sys_local_task_message")
public class LocalTaskMessagePo extends JpaEntity {

    /**
     * 任务ID（唯一标识）
     */
    private String taskId;

    /**
     * 任务名称（可选，用于描述）
     */
    private String taskName;

    /**
     * 任务状态
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LocalTaskStatus status = LocalTaskStatus.PENDING;

    /**
     * 任务类型（决定使用哪个 Processor 处理）
     * HTTP_NOTIFY → HttpNotifyProcessor
     * MQ_NOTIFY → MqNotifyProcessor
     * EMAIL_NOTIFY → EmailNotifyProcessor
     */
    @Column(length = 50)
    private String taskType = "DEFAULT";

    /**
     * 业务ID（幂等 key，用于去重）
     */
    @Column(length = 100)
    private String businessId;

    /**
     * 任务参数（JSON 格式，存储 Processor 所需的所有数据）
     * 不同类型任务的参数结构不同，由各自的 Processor 解析
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, Object> payload;

    /**
     * 已重试次数
     */
    private int retryCount = 0;

    /**
     * 最大重试次数
     */
    private int maxRetry = 3;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 最后一次错误信息
     */
    @Column(length = 1000)
    private String errorMessage;

    /**
     * 处理开始时间（用于检测超时）
     */
    private LocalDateTime processedAt;

    /**
     * 分片编号（用于分片并发）
     */
    private Integer houseNumber;

    /**
     * 任务超时时间（分钟）
     * 默认 5 分钟，超过此时间未完成的任务会被重置为 PENDING
     */
    private int timeoutMinutes = 5;

    // 手动添加 getter/setter 方法（Lombok 不工作）
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public LocalTaskStatus getStatus() {
        return status;
    }

    public void setStatus(LocalTaskStatus status) {
        this.status = status;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Integer getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(Integer houseNumber) {
        this.houseNumber = houseNumber;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

}
