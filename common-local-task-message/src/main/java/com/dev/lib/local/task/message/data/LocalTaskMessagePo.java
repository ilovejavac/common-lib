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
    @Column(length = 100)
    private String taskName;

    /**
     * 服务名称（应用名，用于标识任务来源）
     */
    @Column(length = 100)
    private String serviceName;

    /**
     * 任务状态
     */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LocalTaskStatus status;

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

}
