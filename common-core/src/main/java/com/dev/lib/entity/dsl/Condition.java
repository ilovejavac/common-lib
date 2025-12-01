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
 * private Integer statusNe;                    // status != ?
 * private List<Integer> statusIn;              // status IN (?)
 *
 * // 注解方式
 * @Condition(type = QueryType.LIKE)
 * private String name;                         // name LIKE %?%
 *
 * // 混合：注解指定字段，后缀指定类型
 * @Condition(field = "userName")
 * private String nameLike;                     // user_name LIKE %?%
 * </pre>
 *
 * <h2>二、Join 查询（需注解指定路径）</h2>
 * <pre>
 * @Condition(field = "customer.name")
 * private String customerNameLike;             // customer.name LIKE %?%
 *
 * @Condition(field = "customer.address.city")
 * private String city;                         // customer.address.city = ?
 * </pre>
 *
 * <h2>三、条件分组</h2>
 * <pre>
 * // 字段类型为自定义类（非 JPA 关联）→ 自动识别为分组
 * private TitleFilter titleFilterOr;           // OR (title = ? AND ...)
 *
 * // 分组内字段
 * public class TitleFilter {
 *     private String title;
 *     private String contentLikeOr;            // OR content LIKE %?%
 * }
 * </pre>
 *
 * <h2>四、子查询</h2>
 * <pre>
 * // 后缀方式：Sub 标记
 * private LogFilter logsExistsSub;             // EXISTS (SELECT 1 FROM log WHERE ...)
 * private LogFilter logsNotExistsSub;          // NOT EXISTS (...)
 * private ItemFilter itemsInSub;               // id IN (SELECT id FROM item WHERE ...)
 * private LogFilter statusEqSubOr;             // OR status = (SELECT ... LIMIT 1)
 *
 * // 注解方式：field 指向 JPA 关联
 * @Condition(field = "logs", type = QueryType.EXISTS)
 * private LogFilter logFilter;                 // EXISTS (...)
 *
 * // 混合：注解补充 select/orderBy
 * @Condition(select = "order.id")
 * private ItemFilter itemsInSub;               // id IN (SELECT order_id FROM item WHERE ...)
 *
 * @Condition(select = "status", orderBy = "logTime")
 * private LogFilter latestStatusEqSub;         // status = (SELECT status FROM log ORDER BY log_time DESC LIMIT 1)
 * </pre>
 *
 * <h2>后缀速查表</h2>
 * <pre>
 * 查询类型：
 *   (无)/Eq, Ne, Gt, Ge, Lt, Le, Like, StartWith, EndWith,
 *   In, NotIn, IsNull, IsNotNull, Exists, NotExists, Between
 *
 * 子查询标记：
 *   Sub
 *
 * 连接操作符：
 *   Or, And(默认可省略)
 *
 * 组合顺序：{field}{Type}{Sub}{Or}
 *   例：logsExistsSubOr, statusInSub, nameLikeOr
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {

    /**
     * 查询类型
     * - 默认 EMPTY 表示从字段名后缀解析
     * - 显式指定时覆盖后缀
     */
    QueryType type() default QueryType.EMPTY;

    /**
     * 目标字段/关联路径
     * - 普通查询：实体字段名，如 "status"
     * - Join 查询：嵌套路径，如 "customer.name", "customer.address.city"
     * - 子查询：JPA 关联字段名，如 "logs", "items"
     * - 为空时从字段名解析
     */
    String field() default "";

    /**
     * 与前一个条件的连接方式
     * - 默认 AND
     * - 可通过后缀 Or 指定
     */
    LogicalOperator operator() default LogicalOperator.AND;

    /**
     * 子查询返回字段（仅子查询有效）
     * - EXISTS/NOT_EXISTS：不需要
     * - IN/NOT_IN：子查询 SELECT 的字段，如 "order.id"
     * - EQ/GT/LIKE 等标量子查询：返回单值的字段
     * - 为空时默认使用关联实体的 ID
     */
    String select() default "";

    /**
     * 子查询排序字段（仅子查询有效）
     * - 用于标量子查询取最新/最旧记录
     * - 如 "logTime", "createdAt"
     */
    String orderBy() default "";

    /**
     * 子查询排序方向
     * - true: DESC（取最新，默认）
     * - false: ASC（取最旧）
     */
    boolean desc() default true;
}