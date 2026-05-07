package com.dev.lib.task.infra.data;

import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.task.domain.TaskRunStatus;
import com.dev.lib.task.domain.TriggerSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Data
@Entity
@DynamicUpdate
@Table(
        name = "sys_task_run",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_task_run_run_id", columnNames = "runId"),
                @UniqueConstraint(name = "uk_task_run_task_trigger", columnNames = {"taskId", "triggerTime"})
        }
)
public class TaskRunPo extends JpaEntity {

    @Column(nullable = false, length = 64, unique = true)
    private String runId;

    @Column(nullable = false, length = 64)
    private String taskId;

    @Column(nullable = false, length = 64)
    private String payloadId;

    @Column(nullable = false)
    private Integer attemptNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TriggerSource triggerSource;

    private LocalDateTime triggerTime;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private LocalDateTime nextRetryAt;

    @Column(length = 128)
    private String workerId;

    private LocalDateTime leaseExpireAt;

    private LocalDateTime heartbeatAt;

    @Column(length = 2000)
    private String errorMessage;

    @Column(columnDefinition = "text")
    private String errorStack;

}
