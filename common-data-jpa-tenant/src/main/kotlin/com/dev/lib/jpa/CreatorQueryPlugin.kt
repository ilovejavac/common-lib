package com.dev.lib.jpa

import com.dev.lib.jpa.entity.JpaEntity
import com.dev.lib.jpa.entity.dsl.plugin.QueryPlugin
import com.dev.lib.security.util.SecurityContextHolder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.PathBuilder

class CreatorQueryPlugin : QueryPlugin {
    override fun getOrder(): Int = 10

    override fun supports(entityClass: Class<out JpaEntity?>?): Boolean {
        return true
    }

    override fun apply(
        path: PathBuilder<*>,
        entityClass: Class<*>
    ): BooleanExpression? {
        if (SecurityContextHolder.isSuperAdmin()) {
            return null
        }

//        return if (TenantEntity::class.java.isAssignableFrom(entityClass)) {
//            path.getNumber("tenantId", Long::class.java)
//                ?.eq(SecurityContextHolder.getTenantId())
//
//        } else {
//            path.getNumber("creatorId", Long::class.java)
//                ?.eq(SecurityContextHolder.getUserId())
//        }

        return path.getNumber("creatorId", Long::class.java)
            ?.eq(SecurityContextHolder.getUserId())
    }
}
