package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache.FieldMeta;
import com.dev.lib.entity.dsl.core.RelationInfo;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 子查询构建器
 * <p>
 * 支持两种子查询：
 * 1. 关联子查询（OneToMany, ManyToOne, ManyToMany, OneToOne）
 * 2. 同表子查询（普通字段，如游标分页）
 * <p>
 * 支持所有 QueryType 的子查询形式：
 * - EXISTS / NOT_EXISTS: EXISTS (SELECT 1 FROM ...)
 * - IN / NOT_IN: field IN (SELECT ... FROM ...)
 * - EQ / NE / GT / GE / LT / LE / LIKE 等: field = (SELECT ... FROM ... LIMIT 1)
 */
public class SubQueryBuilder {

    private SubQueryBuilder() {

    }

    /**
     * 构建子查询表达式
     */
    public static BooleanExpression build(
            PathBuilder<?> parentPath,
            FieldMeta subQueryMeta,
            Object filterValue
    ) {

        if (filterValue == null) {
            return null;
        }

        QueryType queryType = subQueryMeta.queryType();

        // 同表子查询
        if (subQueryMeta.isSelfSubQuery()) {
            return buildSelfSubQuery(
                    parentPath,
                    subQueryMeta,
                    filterValue,
                    queryType
            );
        }

        // 关联子查询
        return buildRelationSubQuery(
                parentPath,
                subQueryMeta,
                filterValue,
                queryType
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // 同表子查询
    // ═══════════════════════════════════════════════════════════════

    /**
     * 构建同表子查询
     * <p>
     * SQL 示例:
     * - id > (SELECT id FROM strategy WHERE biz_id = ?)
     * - status = (SELECT status FROM strategy WHERE biz_id = ? ORDER BY create_at DESC LIMIT 1)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BooleanExpression buildSelfSubQuery(
            PathBuilder<?> parentPath,
            FieldMeta subQueryMeta,
            Object filterValue,
            QueryType queryType
    ) {

        Class<?> targetEntity = subQueryMeta.targetEntityClass();

        // 获取子查询实体路径（同表，使用不同别名避免冲突）
        String         subAlias = parentPath.getMetadata().getName() + "_sub";
        PathBuilder<?> subPath  = new PathBuilder<>(
                targetEntity,
                subAlias
        );

        // 构建过滤条件（无关联条件，直接使用用户提供的过滤条件）
        BooleanExpression filterCondition = buildFilterCondition(
                subPath,
                subQueryMeta.filterMetas(),
                filterValue
        );

        String select      = subQueryMeta.select();
        String parentField = subQueryMeta.parentField();  // @Condition(field = "id") 指定的主表字段

        // 根据查询类型构建不同的子查询
        return switch (queryType) {
            case EXISTS -> buildSelfExistsQuery(subPath, filterCondition);
            case NOT_EXISTS -> buildSelfNotExistsQuery(subPath, filterCondition);
            case IN -> buildSelfInQuery(parentPath, subPath, filterCondition, select, parentField);
            case NOT_IN -> buildSelfNotInQuery(
                    parentPath,
                    subPath,
                    filterCondition,
                    select,
                    parentField
            );
            default -> buildSelfScalarQuery(
                    parentPath,
                    subPath,
                    filterCondition,
                    subQueryMeta,
                    queryType
            );
        };
    }

    /**
     * 同表 EXISTS 子查询
     */
    private static BooleanExpression buildSelfExistsQuery(
            PathBuilder<?> subPath,
            BooleanExpression condition
    ) {

        JPQLQuery<?> subQuery = JPAExpressions
                .selectOne()
                .from(subPath)
                .where(condition);
        return subQuery.exists();
    }

    /**
     * 同表 NOT EXISTS 子查询
     */
    private static BooleanExpression buildSelfNotExistsQuery(
            PathBuilder<?> subPath,
            BooleanExpression condition
    ) {

        JPQLQuery<?> subQuery = JPAExpressions
                .selectOne()
                .from(subPath)
                .where(condition);
        return subQuery.notExists();
    }

    /**
     * 同表 IN 子查询
     */
    private static BooleanExpression buildSelfInQuery(
            PathBuilder<?> parentPath,
            PathBuilder<?> subPath,
            BooleanExpression condition,
            String select,
            String parentField
    ) {

        JPQLQuery<?> subQuery = JPAExpressions
                .select(subPath.get(select))
                .from(subPath)
                .where(condition);

        return parentPath.get(parentField).in(subQuery);
    }

    /**
     * 同表 NOT IN 子查询
     */
    private static BooleanExpression buildSelfNotInQuery(
            PathBuilder<?> parentPath,
            PathBuilder<?> subPath,
            BooleanExpression condition,
            String select,
            String parentField
    ) {

        JPQLQuery<?> subQuery = JPAExpressions
                .select(subPath.get(select))
                .from(subPath)
                .where(condition);

        return parentPath.get(parentField).notIn(subQuery);
    }

    /**
     * 同表标量子查询（EQ / NE / GT / GE / LT / LE / LIKE 等）
     * <p>
     * SQL: parent.parentField > (SELECT sub.select FROM sub WHERE conditions)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BooleanExpression buildSelfScalarQuery(
            PathBuilder<?> parentPath,
            PathBuilder<?> subPath,
            BooleanExpression condition,
            FieldMeta subQueryMeta,
            QueryType queryType
    ) {

        String  select      = subQueryMeta.select();
        String  parentField = subQueryMeta.parentField();
        String  orderBy     = subQueryMeta.orderBy();
        boolean desc        = subQueryMeta.desc();

        BooleanExpression finalCondition = condition;

        // 如果有 orderBy，添加 MAX/MIN 条件来模拟 LIMIT 1
        if (StringUtils.hasText(orderBy)) {
            finalCondition = addSelfLatestCondition(
                    subPath,
                    condition,
                    orderBy,
                    desc
            );
        }

        // 构建子查询
        Expression<?> selectExpr = subPath.get(select);
        JPQLQuery<?> subQuery = JPAExpressions
                .select(selectExpr)
                .from(subPath)
                .where(finalCondition);

        // 根据查询类型构建比较表达式
        return buildComparisonExpression(
                parentPath,
                parentField,
                subQuery,
                queryType
        );
    }

    /**
     * 同表子查询的 orderBy 处理（使用 MAX/MIN 模拟 LIMIT 1）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BooleanExpression addSelfLatestCondition(
            PathBuilder<?> subPath,
            BooleanExpression condition,
            String orderByField,
            boolean desc
    ) {

        String         innerAlias = subPath.getMetadata().getName() + "_inner";
        PathBuilder<?> innerPath  = new PathBuilder<>(
                subPath.getType(),
                innerAlias
        );

        ComparablePath<Comparable> orderPath      = subPath.getComparable(orderByField, Comparable.class);
        ComparablePath<Comparable> innerOrderPath = innerPath.getComparable(orderByField, Comparable.class);

        Expression<Comparable> aggregateExpr = desc ? innerOrderPath.max() : innerOrderPath.min();

        // 构建内层子查询（取最大/最小值）
        JPQLQuery<Comparable> innerSubQuery = JPAExpressions
                .select(aggregateExpr)
                .from(innerPath);

        // 如果外层有条件，内层也需要相同条件
        // 注意：这里简化处理，实际可能需要复制条件到内层
        if (condition != null) {
            // 内层条件需要替换别名，这里简化为不加条件
            // 复杂场景可能需要条件克隆
        }

        BooleanExpression latestCondition = orderPath.eq(innerSubQuery);

        return condition == null ? latestCondition : condition.and(latestCondition);
    }

    // ═══════════════════════════════════════════════════════════════
    // 关联子查询（原有逻辑）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 构建关联子查询
     */
    private static BooleanExpression buildRelationSubQuery(
            PathBuilder<?> parentPath,
            FieldMeta subQueryMeta,
            Object filterValue,
            QueryType queryType
    ) {

        RelationInfo relationInfo = subQueryMeta.relationInfo();
        Class<?>     targetEntity = relationInfo.getTargetEntity();

        // 获取子查询实体路径
        EntityPathBase<?> subEntityPath = EntityPathManager.getEntityPath(targetEntity);
        PathBuilder<?> subPath = new PathBuilder<>(
                subEntityPath.getType(),
                subEntityPath.getMetadata()
        );

        // 构建关联条件
        BooleanExpression joinCondition = buildJoinCondition(
                parentPath,
                subPath,
                relationInfo
        );

        // 构建过滤条件
        BooleanExpression filterCondition = buildFilterCondition(
                subPath,
                subQueryMeta.filterMetas(),
                filterValue
        );

        // 合并条件
        BooleanExpression fullCondition = joinCondition;
        if (filterCondition != null) {
            fullCondition = fullCondition.and(filterCondition);
        }

        // 根据查询类型构建子查询
        return switch (queryType) {
            case EXISTS -> buildExistsQuery(subPath, fullCondition);
            case NOT_EXISTS -> buildNotExistsQuery(subPath, fullCondition);
            case IN -> buildInQuery(
                    parentPath,
                    subPath,
                    fullCondition,
                    subQueryMeta
            );
            case NOT_IN -> buildNotInQuery(
                    parentPath,
                    subPath,
                    fullCondition,
                    subQueryMeta
            );
            default -> buildScalarQuery(
                    parentPath,
                    subPath,
                    fullCondition,
                    subQueryMeta,
                    queryType
            );
        };
    }

    /**
     * 构建关联条件
     */
    private static BooleanExpression buildJoinCondition(
            PathBuilder<?> parentPath,
            PathBuilder<?> subPath,
            RelationInfo relationInfo
    ) {

        String joinField = relationInfo.getJoinField();

        return switch (relationInfo.getRelationType()) {
            case ONE_TO_MANY, ONE_TO_ONE -> {
                PathBuilder<?> subJoinPath = subPath.get(joinField);
                yield subJoinPath.get("id").eq(parentPath.get("id"));
            }
            case MANY_TO_ONE, MANY_TO_MANY ->
                    parentPath.get(relationInfo.getFieldName()).get("id").eq(subPath.get("id"));
        };
    }

    /**
     * 构建过滤条件
     */
    private static BooleanExpression buildFilterCondition(
            PathBuilder<?> subPath,
            List<FieldMeta> filterMetas,
            Object filterValue
    ) {

        if (filterMetas == null || filterMetas.isEmpty()) {
            return null;
        }

        BooleanExpression result = null;

        for (FieldMeta meta : filterMetas) {
            if (!meta.isCondition()) continue;

            Object value = meta.getValue(filterValue);
            if (value == null) continue;

            BooleanExpression expr = ExpressionBuilder.build(
                    subPath,
                    meta.targetField(),
                    Optional.ofNullable(meta.queryType()).orElse(QueryType.EQ),
                    value
            );

            if (expr != null) {
                result = result == null ? expr : result.and(expr);
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // 关联子查询 - EXISTS / NOT EXISTS
    // ═══════════════════════════════════════════════════════════════

    private static BooleanExpression buildExistsQuery(
            PathBuilder<?> subPath,
            BooleanExpression condition
    ) {

        JPQLQuery<?> subQuery = JPAExpressions
                .selectOne()
                .from(subPath)
                .where(condition);
        return subQuery.exists();
    }

    private static BooleanExpression buildNotExistsQuery(
            PathBuilder<?> subPath,
            BooleanExpression condition
    ) {

        JPQLQuery<?> subQuery = JPAExpressions
                .selectOne()
                .from(subPath)
                .where(condition);
        return subQuery.notExists();
    }

    // ═══════════════════════════════════════════════════════════════
    // 关联子查询 - IN / NOT IN
    // ═══════════════════════════════════════════════════════════════

    /**
     * IN 子查询
     * <p>
     * SQL: parent.field IN (SELECT sub.selectField FROM sub WHERE ...)
     */
    private static BooleanExpression buildInQuery(
            PathBuilder<?> parentPath,
            PathBuilder<?> subPath,
            BooleanExpression condition,
            FieldMeta subQueryMeta
    ) {

        String select         = subQueryMeta.select();
        String parentField    = resolveParentField(select);
        String subSelectField = resolveSubSelectField(
                select,
                subQueryMeta.relationInfo()
        );

        JPQLQuery<?> subQuery = JPAExpressions
                .select(subPath.get(subSelectField))
                .from(subPath)
                .where(condition);

        return parentPath.get(parentField).in(subQuery);
    }

    private static BooleanExpression buildNotInQuery(
            PathBuilder<?> parentPath,
            PathBuilder<?> subPath,
            BooleanExpression condition,
            FieldMeta subQueryMeta
    ) {

        String select         = subQueryMeta.select();
        String parentField    = resolveParentField(select);
        String subSelectField = resolveSubSelectField(
                select,
                subQueryMeta.relationInfo()
        );

        JPQLQuery<?> subQuery = JPAExpressions
                .select(subPath.get(subSelectField))
                .from(subPath)
                .where(condition);

        return parentPath.get(parentField).notIn(subQuery);
    }

    // ═══════════════════════════════════════════════════════════════
    // 关联子查询 - 标量子查询: EQ / NE / GT / GE / LT / LE / LIKE 等
    // ═══════════════════════════════════════════════════════════════

    /**
     * 标量子查询
     * <p>
     * SQL: parent.field = (SELECT sub.selectField FROM sub WHERE ... ORDER BY orderBy LIMIT 1)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BooleanExpression buildScalarQuery(
            PathBuilder<?> parentPath,
            PathBuilder<?> subPath,
            BooleanExpression condition,
            FieldMeta subQueryMeta,
            QueryType queryType
    ) {

        String select = subQueryMeta.select();
        if (!StringUtils.hasText(select)) {
            // 没有 select 字段，退化为 EXISTS
            return buildExistsQuery(
                    subPath,
                    condition
            );
        }

        String  parentField    = resolveParentField(select);
        String  subSelectField = resolveSubSelectField(
                select,
                subQueryMeta.relationInfo()
        );
        String  orderBy        = subQueryMeta.orderBy();
        boolean desc           = subQueryMeta.desc();

        // 构建子查询
        BooleanExpression finalCondition = condition;

        // 如果有 orderBy，添加"取最新/最旧"逻辑
        if (StringUtils.hasText(orderBy)) {
            finalCondition = addLatestCondition(
                    subPath,
                    condition,
                    subQueryMeta.relationInfo(),
                    orderBy,
                    desc
            );
        }

        Expression<?> selectExpr = subPath.get(subSelectField);
        JPQLQuery<?> subQuery = JPAExpressions
                .select(selectExpr)
                .from(subPath)
                .where(finalCondition);

        // 根据查询类型构建比较表达式
        return buildComparisonExpression(
                parentPath,
                parentField,
                subQuery,
                queryType
        );
    }

    /**
     * 添加"取最新/最旧"条件（关联子查询）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BooleanExpression addLatestCondition(
            PathBuilder<?> subPath,
            BooleanExpression condition,
            RelationInfo relationInfo,
            String orderByField,
            boolean desc
    ) {

        String         innerAlias = subPath.getMetadata().getName() + "_inner";
        PathBuilder<?> innerPath  = new PathBuilder<>(
                subPath.getType(),
                innerAlias
        );

        // 内层与外层关联
        String joinField = relationInfo.getJoinField();
        BooleanExpression innerJoin = innerPath.get(joinField).get("id")
                .eq(subPath.get(joinField).get("id"));

        // MAX / MIN 子查询
        ComparablePath<Comparable> orderPath      = subPath.getComparable(
                orderByField,
                Comparable.class
        );
        ComparablePath<Comparable> innerOrderPath = innerPath.getComparable(
                orderByField,
                Comparable.class
        );

        Expression<Comparable> aggregateExpr = desc
                                               ? innerOrderPath.max()
                                               : innerOrderPath.min();

        // 构建子查询
        BooleanExpression latestCondition = orderPath.eq(
                JPAExpressions
                        .select(aggregateExpr)
                        .from(innerPath)
                        .where(innerJoin)
        );

        return condition == null ? latestCondition : condition.and(latestCondition);
    }

    /**
     * 构建比较表达式
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BooleanExpression buildComparisonExpression(
            PathBuilder<?> parentPath,
            String parentField,
            JPQLQuery<?> subQuery,
            QueryType queryType
    ) {

        return switch (queryType) {
            case EQ -> parentPath.get(parentField).eq(subQuery);
            case NE -> parentPath.get(parentField).ne(subQuery);
            case GT -> parentPath.getComparable(
                    parentField,
                    Comparable.class
            ).gt((Expression) subQuery);
            case GE -> parentPath.getComparable(
                    parentField,
                    Comparable.class
            ).goe((Expression) subQuery);
            case LT -> parentPath.getComparable(
                    parentField,
                    Comparable.class
            ).lt((Expression) subQuery);
            case LE -> parentPath.getComparable(
                    parentField,
                    Comparable.class
            ).loe((Expression) subQuery);
            case LIKE -> {
                StringPath stringPath = parentPath.getString(parentField);
                yield stringPath.like((Expression<String>) subQuery);
            }
            default -> parentPath.get(parentField).eq(subQuery);
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════════════════════════

    /**
     * 解析父表字段
     * <p>
     * select = "order.id" → 返回 "id"（主表的 id）
     * select = "status" → 返回 "status"
     */
    private static String resolveParentField(String select) {

        if (!StringUtils.hasText(select)) {
            return "id";
        }
        if (select.contains(".")) {
            // 格式: parent.field → 取第二部分
            return select.substring(select.indexOf(".") + 1);
        }
        return select;
    }

    /**
     * 解析子查询 SELECT 字段
     * <p>
     * select = "order.id" → 返回 "order.id"（子表指向主表的关联字段.id）
     * select = "status" → 返回 "status"
     */
    private static String resolveSubSelectField(String select, RelationInfo relationInfo) {

        if (!StringUtils.hasText(select)) {
            // 默认返回关联字段的 id
            String joinField = relationInfo.getJoinField();
            return joinField + ".id";
        }
        if (select.contains(".")) {
            // 格式: parent.field → 子表中对应的是 joinField.field
            String field = select.substring(select.indexOf(".") + 1);
            return relationInfo.getJoinField() + "." + field;
        }
        return select;
    }

}