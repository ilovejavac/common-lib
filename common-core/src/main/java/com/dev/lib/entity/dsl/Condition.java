package com.dev.lib.entity.dsl;

import com.dev.lib.entity.dsl.group.LogicalOperator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 统一条件注解
 *
 * 注解与字段名后缀两套配置方式，可二选一或互补：
 * - 注解优先级高于后缀
 * - 未指定的属性从后缀解析
 *
 * <h2>一、普通条件查询</h2>
 * <pre>
 * // 后缀方式
 * private String nameLike;                     // name LIKE %?%
 *
 * // 注解方式
 * @Condition(type = QueryType.LIKE)
 * private String name;                         // name LIKE %?%
 * </pre>
 *
 * <h2>二、Join 查询</h2>
 * <pre>
 * @Condition(field = "customer.name")
 * private String customerNameLike;             // customer.name LIKE %?%
 * </pre>
 *
 * <h2>三、条件分组</h2>
 * <pre>
 * private TitleFilter titleFilterOr;           // OR (...)
 * </pre>
 *
 * <h2>四、子查询</h2>
 * <pre>
 * // 后缀方式
 * private LogFilter logsExistsSub;             // EXISTS (...)
 *
 * // 注解方式
 * @Condition(field = "logs", type = QueryType.EXISTS)
 * private LogFilter logFilter;
 *
 * // 标量子查询
 * @Condition(select = "status", orderBy = "logTime")
 * private LogFilter latestStatusEqSub;         // status = (SELECT ... LIMIT 1)
 * </pre>
 *
 * <h2>后缀格式</h2>
 * <pre>
 * {field}{Type}{Sub?}{Or?}
 * 例：nameLikeOr, logsExistsSub, statusInSubOr
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {

    /**
     * 查询类型，默认从后缀解析
     */
    QueryType type() default QueryType.EMPTY;

    /**
     * 目标字段/关联路径
     */
    String field() default "";

    /**
     * 连接方式
     */
    LogicalOperator operator() default LogicalOperator.AND;

    /**
     * 子查询返回字段
     */
    String select() default "";

    /**
     * 子查询排序字段
     */
    String orderBy() default "";

    /**
     * 排序方向（true=DESC, false=ASC）
     */
    boolean desc() default true;
}