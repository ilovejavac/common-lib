package com.dev.lib.jpa

import com.dev.lib.jpa.entity.JpaEntity
import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
@EntityListeners(TenantEntityListener::class)
abstract class TenantEntity(
    @Column(nullable = false, updatable = false)
    var tenantId: Long = 0L
) : JpaEntity()
