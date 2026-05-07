package com.dev.lib.task.infra.data;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity
@DynamicUpdate
@Table(name = "sys_task_payload")
public class TaskPayloadPo extends JpaEntity {

    @Column(nullable = false, length = 64, unique = true)
    private String payloadId;

    @Column(nullable = false, length = 64)
    private String taskId;

    @Column(nullable = false, length = 255)
    private String payloadClass;

    @Column(nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(length = 128)
    private String payloadHash;

    @Column(nullable = false)
    private Integer version = 1;

}
