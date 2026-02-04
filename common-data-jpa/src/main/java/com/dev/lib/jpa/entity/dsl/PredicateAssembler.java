package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.FieldMetaCache.FieldMeta;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.AllArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PredicateAssembler {

    private PredicateAssembler() {

    }

    @SuppressWarnings("unchecked")
    private static <T extends JpaEntity> EntityPathBase<T> getEntityPath(Class<?> clazz) {

        return (EntityPathBase<T>) EntityPathManager.getEntityPath(
                FieldMetaCache.getMeta(clazz).entityClass()
        );
    }

    /**
     * 组装查询条件
     */
    public static <E extends JpaEntity> BooleanBuilder assemble(
            DslQuery<E> query,
            Collection<QueryFieldMerger.FieldMetaValue> fields,
            BooleanExpression... expressions
    ) {

        BooleanBuilder builder = new BooleanBuilder();

        if (query != null) {
            EntityPathBase<E> entityPath = getEntityPath(query.getClass());
            PathBuilder<E> pathBuilder = new PathBuilder<>(
                    entityPath.getType(),
                    entityPath.getMetadata()
            );

            // ========== 原有逻辑 ==========
            if (!CollectionUtils.isEmpty(fields)) {
                List<ExpressionItem> items     = collectExpressions(pathBuilder, fields);
                Predicate            predicate = buildWithPrecedence(items);
                Optional.ofNullable(predicate).ifPresent(builder::and);
            }
        }

        for (BooleanExpression expression : expressions) {
            builder.and(expression);
        }

        return builder;
    }

    /**
     * 收集表达式
     */
    private static <E extends JpaEntity> List<ExpressionItem> collectExpressions(
            PathBuilder<E> pathBuilder,
            Collection<QueryFieldMerger.FieldMetaValue> fields
    ) {

        List<ExpressionItem> items = new ArrayList<>();

        for (QueryFieldMerger.FieldMetaValue fv : fields) {
            FieldMeta fm    = fv.getFieldMeta();
            Object    value = fv.getValue();

            if (value == null) continue;

            switch (fm.metaType()) {
                case CONDITION -> {
                    BooleanExpression expr = ExpressionBuilder.build(
                            pathBuilder,
                            fm.targetField(),
                            Optional.ofNullable(fm.queryType()).orElse(QueryType.EQ),
                            value
                    );
                    if (expr != null) {
                        items.add(new ExpressionItem(expr, fm.operator()));
                    }
                }
                case GROUP -> {
                    Predicate groupPredicate = buildGroupPredicate(pathBuilder, fm, value);
                    if (groupPredicate != null) {
                        items.add(new ExpressionItem(groupPredicate, fm.operator()));
                    }
                }
                case SUB_QUERY -> {
                    BooleanExpression subExpr = SubQueryBuilder.build(pathBuilder, fm, value);
                    if (subExpr != null) {
                        items.add(new ExpressionItem(subExpr, fm.operator()));
                    }
                }
            }
        }

        return items;
    }

    private static <E extends JpaEntity> Predicate buildGroupPredicate(
            PathBuilder<E> pathBuilder,
            FieldMeta groupMeta,
            Object groupValue
    ) {

        List<FieldMeta> nestedMetas = groupMeta.nestedMetas();
        if (nestedMetas == null || nestedMetas.isEmpty()) {
            return null;
        }

        List<QueryFieldMerger.FieldMetaValue> nestedFields = new ArrayList<>();
        for (FieldMeta nested : nestedMetas) {
            Object nestedValue = nested.getValue(groupValue);
            if (nestedValue != null) {
                nestedFields.add(new QueryFieldMerger.FieldMetaValue(nestedValue, nested));
            }
        }

        if (nestedFields.isEmpty()) {
            return null;
        }

        List<ExpressionItem> nestedItems = collectExpressions(pathBuilder, nestedFields);
        if (nestedItems.isEmpty()) {
            return null;
        }

        return buildWithPrecedence(nestedItems);
    }

    private static Predicate buildWithPrecedence(List<ExpressionItem> items) {

        if (items.isEmpty()) return null;
        if (items.size() == 1) return items.getFirst().expression;

        List<List<Predicate>> andGroups    = new ArrayList<>();
        List<Predicate>        currentGroup = new ArrayList<>();

        for (ExpressionItem item : items) {
            if (item.operator == LogicalOperator.OR) {
                // 遇到 OR，先保存当前 group（不包括 OR 条件本身）
                if (!currentGroup.isEmpty()) {
                    andGroups.add(new ArrayList<>(currentGroup));
                    currentGroup.clear();
                }
                // OR 条件单独成一组
                andGroups.add(List.of(item.expression));
            } else {
                // AND 条件加入当前组
                currentGroup.add(item.expression);
            }
        }

        // 处理最后一个 group
        if (!currentGroup.isEmpty()) {
            andGroups.add(currentGroup);
        }

        // 每个 group 内部用 AND 连接，group 之间用 OR 连接
        List<Predicate> groupPredicates = new ArrayList<>();
        for (List<Predicate> group : andGroups) {
            BooleanBuilder andBuilder = new BooleanBuilder();
            for (Predicate pred : group) {
                andBuilder.and(pred);
            }
            if (andBuilder.getValue() != null) {
                groupPredicates.add(andBuilder.getValue());
            }
        }

        if (groupPredicates.isEmpty()) return null;
        if (groupPredicates.size() == 1) return groupPredicates.getFirst();

        BooleanBuilder result = new BooleanBuilder();
        for (Predicate pred : groupPredicates) {
            result.or(pred);
        }
        return result.getValue();
    }

    @AllArgsConstructor
    private static class ExpressionItem {

        Predicate expression;

        LogicalOperator operator;

    }

}
