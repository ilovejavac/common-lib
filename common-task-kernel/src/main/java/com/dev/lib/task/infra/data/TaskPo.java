package com.dev.lib.task.infra.data;

import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.task.domain.TaskMode;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Data
@Entity
@DynamicUpdate
@Table(name = "sys_task")
public class TaskPo extends JpaEntity {

    @Column(nullable = false, length = 64, unique = true)
    private String taskId;

    @Column(nullable = false, length = 100)
    private String application;

    @Column(nullable = false, length = 100)
    private String taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status;

    @Column(length = 64)
    private String payloadId;

    @Column(length = 120)
    private String cron;

    private LocalDateTime runAt;

    @Column(columnDefinition = "text")
    private String retryPolicyJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TaskStatus onExhaustedStatus;

    @Column(length = 64)
    private String lastRunId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskRunStatus lastRunStatus;

    private LocalDateTime lastRunAt;

    private LocalDateTime nextRunAt;

    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 128)
    private String configHash;

}
