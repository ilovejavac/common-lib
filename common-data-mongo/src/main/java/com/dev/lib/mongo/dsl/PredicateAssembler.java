package com.dev.lib.mongo.dsl;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.FieldMetaCache.FieldMeta;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.dev.lib.mongo.MongoEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
public final class PredicateAssembler {

    private PredicateAssembler() {

    }

    @SuppressWarnings("unchecked")
    public static <E extends MongoEntity> BooleanBuilder assemble(
            DslQuery<E> query,
            Collection<QueryFieldMerger.FieldMetaValue> fields,
            BooleanExpression... expressions
    ) {

        BooleanBuilder builder = new BooleanBuilder();

        if (query != null && fields != null && !fields.isEmpty()) {
            Class<?>          entityClass = FieldMetaCache.getMeta(query.getClass()).entityClass();
            EntityPathBase<E> entityPath  = EntityPathManager.getEntityPath((Class<E>) entityClass);
            PathBuilder<E>    pathBuilder = new PathBuilder<>(
                    entityPath.getType(),
                    entityPath.getMetadata()
            );

            List<ExpressionItem> items     = collectExpressions(
                    pathBuilder,
                    fields
            );
            Predicate            predicate = buildWithPrecedence(items);
            if (predicate != null) {
                builder.and(predicate);
            }
        }

        for (BooleanExpression expr : expressions) {
            if (expr != null) {
                builder.and(expr);
            }
        }

        return builder;
    }

    private static <E extends MongoEntity> List<ExpressionItem> collectExpressions(
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
                        items.add(new ExpressionItem(
                                expr,
                                fm.operator()
                        ));
                    }
                }

                case GROUP -> {
                    Predicate groupPred = buildGroupPredicate(
                            pathBuilder,
                            fm,
                            value
                    );
                    if (groupPred != null) {
                        items.add(new ExpressionItem(
                                groupPred,
                                fm.operator()
                        ));
                    }
                }

                case SUB_QUERY -> {
                    // MongoDB 嵌入文档查询（替代 JPA 子查询）
                    Predicate embeddedPred = buildEmbeddedPredicate(
                            pathBuilder,
                            fm,
                            value
                    );
                    if (embeddedPred != null) {
                        items.add(new ExpressionItem(
                                embeddedPred,
                                fm.operator()
                        ));
                    }
                }
            }
        }
        return items;
    }

    private static <E extends MongoEntity> Predicate buildGroupPredicate(
            PathBuilder<E> pathBuilder,
            FieldMeta groupMeta,
            Object groupValue
    ) {

        List<FieldMeta> nestedMetas = groupMeta.nestedMetas();
        if (nestedMetas == null || nestedMetas.isEmpty()) return null;

        List<QueryFieldMerger.FieldMetaValue> nestedFields = new ArrayList<>();
        for (FieldMeta nested : nestedMetas) {
            Object nestedValue = nested.getValue(groupValue);
            if (nestedValue != null) {
                nestedFields.add(new QueryFieldMerger.FieldMetaValue(
                        nestedValue,
                        nested
                ));
            }
        }

        if (nestedFields.isEmpty()) return null;

        List<ExpressionItem> nestedItems = collectExpressions(
                pathBuilder,
                nestedFields
        );
        return buildWithPrecedence(nestedItems);
    }

    /**
     * 嵌入文档查询（替代 JPA 子查询）
     */
    private static <E extends MongoEntity> Predicate buildEmbeddedPredicate(
            PathBuilder<E> pathBuilder,
            FieldMeta fm,
            Object filterValue
    ) {
        // 推断嵌入文档路径
        String embeddedPath = resolveEmbeddedPath(fm);

        if (embeddedPath == null) {
            log.warn(
                    "MongoDB 嵌入文档查询无法推断路径，已忽略: {}",
                    fm.field().getName()
            );
            return null;
        }

        List<FieldMeta> filterMetas = fm.filterMetas();
        if (filterMetas == null || filterMetas.isEmpty()) return null;

        BooleanBuilder builder = new BooleanBuilder();
        for (FieldMeta nested : filterMetas) {
            if (!nested.isCondition()) continue;

            Object nestedValue = nested.getValue(filterValue);
            if (nestedValue == null) continue;

            // 拼接完整路径: items.price
            String fullPath = embeddedPath + "." + nested.targetField();
            BooleanExpression expr = ExpressionBuilder.build(
                    pathBuilder,
                    fullPath,
                    Optional.ofNullable(nested.queryType()).orElse(QueryType.EQ),
                    nestedValue
            );
            if (expr != null) {
                builder.and(expr);
            }
        }

        return builder.getValue();
    }

    /**
     * 推断嵌入文档路径（MongoDB 专用）
     */
    private static String resolveEmbeddedPath(FieldMeta fm) {
        // 1. 关联子查询（JPA）- MongoDB 不支持，返回 null
        if (fm.relationInfo() != null) {
            log.warn(
                    "MongoDB 不支持 JPA 关联子查询，已忽略: {}",
                    fm.field().getName()
            );
            return null;
        }

        // 2. 同表子查询的 parentField（可能是 select 属性或推断值）
        if (fm.parentField() != null && !fm.parentField().isEmpty()) {
            return fm.parentField();
        }

        // 3. 从字段名推断：itemsSub → items
        String fieldName = fm.field().getName();
        if (fieldName.endsWith("ExistsSub")) {
            return fieldName.substring(
                    0,
                    fieldName.length() - 9
            );
        }
        if (fieldName.endsWith("NotExistsSub")) {
            return fieldName.substring(
                    0,
                    fieldName.length() - 12
            );
        }
        if (fieldName.endsWith("Sub")) {
            return fieldName.substring(
                    0,
                    fieldName.length() - 3
            );
        }

        // 4. 无法推断
        return null;
    }

    private static Predicate buildWithPrecedence(List<ExpressionItem> items) {

        if (items.isEmpty()) return null;
        if (items.size() == 1) return items.get(0).expression;

        // 按 OR 分组，实现 AND 优先级高于 OR
        List<List<ExpressionItem>> andGroups    = new ArrayList<>();
        List<ExpressionItem>       currentGroup = new ArrayList<>();

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

        // 组内 AND，组间 OR
        List<Predicate> groupPredicates = new ArrayList<>();
        for (List<ExpressionItem> group : andGroups) {
            BooleanBuilder andBuilder = new BooleanBuilder();
            for (ExpressionItem item : group) {
                andBuilder.and(item.expression);
            }
            if (andBuilder.getValue() != null) {
                groupPredicates.add(andBuilder.getValue());
            }
        }

        if (groupPredicates.isEmpty()) return null;
        if (groupPredicates.size() == 1) return groupPredicates.get(0);

        BooleanBuilder result = new BooleanBuilder();
        for (Predicate pred : groupPredicates) {
            result.or(pred);
        }
        return result.getValue();
    }

    private record ExpressionItem(Predicate expression, LogicalOperator operator) {
    }

}
