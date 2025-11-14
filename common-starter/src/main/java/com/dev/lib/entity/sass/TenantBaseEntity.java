package com.dev.lib.entity.sass;

import com.dev.lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多租户
 */
@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TenantBaseEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}