package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.BaseEntity;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.ConditionIgnore;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.dev.lib.entity.sass.TenantBaseEntity;
import com.dev.lib.security.util.SecurityContextHolder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.AllArgsConstructor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PredicateAssembler {
    private PredicateAssembler() {}

    public static <E extends BaseEntity> BooleanBuilder assemble(
            DslQuery<E> query,
            Map<String, QueryFieldMerger.ExternalFieldInfo> externalFields,
            BooleanExpression... expressions
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        if (query != null) {
            PathBuilder<E> pathBuilder = new PathBuilder<>(
                    query.getEntityPath().getType(),
                    query.getEntityPath().getMetadata()
            );

            List<ExpressionItem> items = collectExpressions(query, pathBuilder, externalFields);
            Predicate result = buildWithPrecedence(items);

            if (result != null) {
                builder.and(result);
            }
        }

        for (BooleanExpression expression : expressions) {
            builder.and(expression);
        }

//        if (query != null && isTenantEntity(query.getEntityPath().getType())) {
//            Long tenantId = SecurityContextHolder.current().getTenant();
//            if (tenantId != null) {
//                PathBuilder<?> pathBuilder = new PathBuilder<>(
//                        query.getEntityPath().getType(),
//                        query.getEntityPath().getMetadata()
//                );
//                builder.and(pathBuilder.get("tenantId").eq(tenantId));
//            }
//        }

        return builder;
    }

    private static boolean isTenantEntity(Class<?> entityClass) {
        return TenantBaseEntity.class.isAssignableFrom(entityClass);
    }

    private static <E extends BaseEntity> List<ExpressionItem> collectExpressions(
            DslQuery<E> query,
            PathBuilder<E> pathBuilder,
            Map<String, QueryFieldMerger.ExternalFieldInfo> externalFields
    ) {
        List<ExpressionItem> items = new ArrayList<>();

        // 1. 处理 DslQuery 自身字段 (包括父类字段)
        List<Field> fields = getAllFields(query.getClass());
        for (Field field : fields) {
            if (shouldSkipField(field)) continue;

            ReflectionUtils.makeAccessible(field);
            Object value = getFieldValue(field, query);
            if (value == null) continue;

            // 处理嵌套 DslQuery
            if (value instanceof DslQuery<?> nestedQuery) {
                Predicate nestedExpr = assemble(
                        nestedQuery,
                        Map.of()
                ).getValue();

                if (nestedExpr != null) {
                    Condition condition = field.getAnnotation(Condition.class);
                    LogicalOperator operator = condition != null
                            ? condition.operator()
                            : LogicalOperator.AND;
                    items.add(new ExpressionItem(nestedExpr, operator));
                }
                continue;
            }

            // 处理普通字段
            Condition condition = field.getAnnotation(Condition.class);
            if (condition == null) continue;

            String targetField = condition.field().isEmpty()
                    ? field.getName()
                    : condition.field();

            BooleanExpression expr = ExpressionBuilder.build(
                    pathBuilder,
                    targetField,
                    condition.type(),
                    value
            );

            if (expr != null) {
                items.add(new ExpressionItem(expr, condition.operator()));
            }
        }

        // 2. 处理外部字段
        for (QueryFieldMerger.ExternalFieldInfo info : externalFields.values()) {
            BooleanExpression expr = ExpressionBuilder.build(
                    pathBuilder,
                    info.getTargetField(),
                    info.getType(),
                    info.getValue()
            );

            if (expr != null) {
                items.add(new ExpressionItem(expr, info.getOperator()));
            }
        }

        return items;
    }

    /**
     * 递归获取类及其所有父类的字段
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }

        return fields;
    }

    private static boolean shouldSkipField(Field field) {
        return field.isAnnotationPresent(JsonIgnore.class)
                || field.isAnnotationPresent(ConditionIgnore.class)
                || field.getName().equals("entityPath")
                || field.getName().equals("externalFields")
                || Modifier.isStatic(field.getModifiers());
    }

    private static Object getFieldValue(Field field, Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Predicate buildWithPrecedence(List<ExpressionItem> items) {
        if (items.isEmpty()) return null;
        if (items.size() == 1) return items.get(0).expression;

        // 分割成 AND 组 (遇到 OR 就分割)
        List<List<ExpressionItem>> andGroups = new ArrayList<>();
        List<ExpressionItem> currentGroup = new ArrayList<>();

        for (ExpressionItem item : items) {
            currentGroup.add(item);

            if (item.operator == LogicalOperator.OR) {
                andGroups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
            }
        }

        if (!currentGroup.isEmpty()) {
            andGroups.add(currentGroup);
        }

        // 构建每个 AND 组
        List<Predicate> groupExpressions = new ArrayList<>();
        for (List<ExpressionItem> group : andGroups) {
            if (group.isEmpty()) continue;

            BooleanBuilder andBuilder = new BooleanBuilder();
            for (ExpressionItem item : group) {
                andBuilder.and(item.expression);
            }

            if (andBuilder.getValue() != null) {
                groupExpressions.add(andBuilder.getValue());
            }
        }

        // OR 连接所有 AND 组
        if (groupExpressions.isEmpty()) return null;
        if (groupExpressions.size() == 1) return groupExpressions.get(0);

        BooleanBuilder result = new BooleanBuilder();
        for (Predicate expr : groupExpressions) {
            result.or(expr);
        }

        return result.getValue();
    }

    @AllArgsConstructor
    private static class ExpressionItem {
        Predicate expression;
        LogicalOperator operator;
    }
}