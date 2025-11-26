package com.dev.lib.jpa.entity.sass;

import com.dev.lib.jpa.entity.JpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

/**
 * 多租户
 */
@MappedSuperclass
@Data
@EntityListeners(TenantEntityListener.class)
public abstract class TenantBaseEntity extends JpaEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
}