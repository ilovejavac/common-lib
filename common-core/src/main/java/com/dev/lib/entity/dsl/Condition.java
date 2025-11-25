package com.dev.lib.entity.dsl;

import com.dev.lib.entity.dsl.group.LogicalOperator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {
    QueryType type() default QueryType.EMPTY;

    String field() default "";  // 空则用字段名

    LogicalOperator operator() default LogicalOperator.AND;  // 新增
}