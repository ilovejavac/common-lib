package com.dev.lib.local.task.message.data;

import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.local.task.message.domain.model.NotifyType;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@DynamicUpdate
@Table(name = "sys_local_task_message")
@AutoMapper(target = TaskMessageEntityCommand.class)
public class LocalTaskMessagePo extends JpaEntity {

    private String taskId;

    private String taskName;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LocalTaskStatus status = LocalTaskStatus.PENDING;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotifyType notifyType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private TaskMessageEntityCommand.NotifyConfig notifyConfig;

    @Column(length = 100)
    private String businessId;        // 业务ID（幂等 key）

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "text")
    private Map<String, Object> payload;  // 任务参数

    private int retryCount = 0;           // 已重试次数

    private int maxRetry = 3;             // 最大重试次数

    private LocalDateTime nextRetryTime;  // 下次重试时间

    @Column(length = 1000)
    private String errorMessage;          // 最后一次错误信息

    private LocalDateTime processedAt;    // 处理完成时间

    private Integer houseNumber;

}
