package com.dev.lib.entity.sass;

import com.dev.lib.security.util.SecurityContextHolder;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.stereotype.Component;

@Component
public class TenantQueryDslFilter {

    public <T extends TenantBaseEntity> BooleanBuilder addTenantCondition(
            EntityPathBase<T> qEntity, BooleanBuilder builder) {

        Long tenantId = SecurityContextHolder.get().getTenant();
        if (tenantId == null) {
            throw new RuntimeException("Tenant ID not found");
        }

        try {
            PathBuilder<T> pathBuilder = new PathBuilder<>(qEntity.getType(), qEntity.getMetadata());
            NumberPath<Long> tenantIdPath = pathBuilder.getNumber("tenantId", Long.class);
            builder.and(tenantIdPath.eq(tenantId));
        } catch (Exception e) {
            // 如果实体没有 tenantId 字段，忽略
        }

        return builder;
    }
}