package com.dev.lib.jpa

import com.dev.lib.security.util.SecurityContextHolder
import jakarta.persistence.PrePersist

class TenantEntityListener {

    @PrePersist
    fun prePersist(entity: TenantEntity) {
        entity.tenantId = SecurityContextHolder.getTenantId();
    }
}