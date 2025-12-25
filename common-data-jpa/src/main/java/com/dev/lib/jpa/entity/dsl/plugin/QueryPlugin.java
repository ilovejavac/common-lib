package com.dev.lib.jpa.entity.dsl.plugin;

import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;

/**
 * 查询插件接口
 * <p>
 * 在查询构建时自动追加条件
 */
public interface QueryPlugin {

    /**
     * 优先级，越小越先执行
     */
    int getOrder();

    /**
     * 是否对该实体类生效
     */
    boolean supports(Class<? extends JpaEntity> entityClass);

    /**
     * 追加查询条件
     *
     * @return null 表示不追加
     */
    BooleanExpression apply(PathBuilder<?> path, Class<?> entityClass);

}
