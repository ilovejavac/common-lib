package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.BaseEntity;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PredicateAssembler {
    private PredicateAssembler() {
    }



    public static <E extends BaseEntity> BooleanBuilder assemble(
            DslQuery<E> query,
            Collection<QueryFieldMerger.FieldMetaValue> fields,
            BooleanExpression... expressions
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        if (query != null && !fields.isEmpty()) {
            PathBuilder<E> pathBuilder = new PathBuilder<>(
                    query.getEntityPath().getType(),
                    query.getEntityPath().getMetadata()
            );

            Predicate predicate = buildWithPrecedence(collectExpressions(pathBuilder, fields));
            Optional.ofNullable(predicate).ifPresent(builder::and);
        }

        for (BooleanExpression expression : expressions) {
            builder.and(expression);
        }

        return builder;
    }

    private static <E extends BaseEntity> List<ExpressionItem> collectExpressions(
            PathBuilder<E> pathBuilder,
            Collection<QueryFieldMerger.FieldMetaValue> fields
    ) {
        List<ExpressionItem> items = new ArrayList<>();

        for (QueryFieldMerger.FieldMetaValue fv : fields) {
            FieldMetaCache.FieldMeta fm = fv.getFieldMeta();
            Object value = fv.getValue();

            // 处理嵌套 DslQuery
            if (fm.isNestedQuery() && value instanceof DslQuery<?> nestedQuery) {
                List<QueryFieldMerger.FieldMetaValue> nestedFields = QueryFieldMerger.resolve(nestedQuery);
                Predicate nestedExpr = assemble(nestedQuery, nestedFields).getValue();

                if (nestedExpr != null) {
                    items.add(new ExpressionItem(nestedExpr, fm.getOperator()));
                }
                continue;
            }

            BooleanExpression expr = ExpressionBuilder.build(
                    pathBuilder,
                    fm.getTargetField(),
                    Optional.ofNullable(fm.getQueryType()).orElse(QueryType.EQ),
                    value
            );

            if (expr != null) {
                items.add(new ExpressionItem(expr, fm.getOperator()));
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