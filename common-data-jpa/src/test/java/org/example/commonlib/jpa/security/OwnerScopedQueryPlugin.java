package org.example.commonlib.jpa.security;

import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.dsl.plugin.QueryPlugin;
import com.dev.lib.security.util.SecurityContextHolder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;

public class OwnerScopedQueryPlugin implements QueryPlugin {

    @Override
    public int getOrder() {

        return 10;
    }

    @Override
    public boolean supports(Class<? extends JpaEntity> entityClass) {

        return ScopedWriteThing.class.equals(entityClass);
    }

    @Override
    public BooleanExpression apply(PathBuilder<?> path, Class<?> entityClass) {

        Long userId = SecurityContextHolder.getUserId();
        if (userId == null || userId <= 0L) {
            return Expressions.asBoolean(false).isTrue();
        }
        return path.getNumber("ownerId", Long.class).eq(userId);
    }
}
