package com.dev.lib.jpa

import com.dev.lib.jpa.entity.JpaEntity
import com.dev.lib.jpa.entity.dsl.plugin.QueryPlugin
import com.dev.lib.security.util.SecurityContextHolder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.PathBuilder

class TenantQueryPlugin : QueryPlugin {
    override fun getOrder(): Int = 0

    override fun supports(entityClass: Class<out JpaEntity>): Boolean {
        return TenantEntity::class.java.isAssignableFrom(entityClass)
    }

    override fun apply(
        path: PathBuilder<*>,
        entityClass: Class<*>
    ): BooleanExpression? {
        if (SecurityContextHolder.isSuperAdmin()) {
            return null
        }

        return path.getNumber("tenantId", Long::class.java)
            ?.eq(SecurityContextHolder.getTenantId())
    }
}