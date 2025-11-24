package com.dev.lib.entity.audit;

import com.dev.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "sys_audit_log", indexes = {
        @Index(name = "idx_entity", columnList = "entity_id"),
        @Index(name = "idx_created", columnList = "created_at")
})
public class AuditLog extends BaseEntity {
    @Column(nullable = false, length = 64)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(columnDefinition = "text")
    private String recordValue;
}