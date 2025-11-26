package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
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

    public static <E extends JpaEntity> BooleanBuilder assemble(
            DslQuery<E> query,
            Collection<QueryFieldMerger.FieldMetaValue> fields,
            BooleanExpression... expressions
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        if (query != null && !CollectionUtils.isEmpty(fields)) {
            PathBuilder<E> pathBuilder = new PathBuilder(
                    getEntityPath(query.getClass()).getType(),
                    getEntityPath(query.getClass()).getMetadata()
            );

            Predicate predicate = buildWithPrecedence(collectExpressions(pathBuilder, fields));
            Optional.ofNullable(predicate).ifPresent(builder::and);
        }

        for (BooleanExpression expression : expressions) {
            builder.and(expression);
        }

        return builder;
    }

    private static <E extends JpaEntity> List<ExpressionItem> collectExpressions(
            PathBuilder<E> pathBuilder,
            Collection<QueryFieldMerger.FieldMetaValue> fields
    ) {
        List<ExpressionItem> items = new ArrayList<>();

        for (QueryFieldMerger.FieldMetaValue fv : fields) {
            FieldMetaCache.FieldMeta fm = fv.getFieldMeta();
            Object value = fv.getValue();

            // 处理嵌套 DslQuery
//            if (fm.isNestedQuery() && value instanceof DslQuery<?> nestedQuery) {
//                List<QueryFieldMerger.FieldMetaValue> nestedFields = QueryFieldMerger.resolve(nestedQuery);
//                Predicate nestedExpr = assemble(nestedQuery, nestedFields).getValue();
//
//                if (nestedExpr != null) {
//                    items.add(new ExpressionItem(nestedExpr, fm.getOperator()));
//                }
//                continue;
//            }

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

        return items;
    }

    private static Predicate buildWithPrecedence(List<ExpressionItem> items) {
        if (items.isEmpty()) return null;
        if (items.size() == 1) return items.get(0).expression;

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