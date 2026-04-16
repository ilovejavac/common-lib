package com.dev.lib.mongo.dsl;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryWhere;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.FieldMetaCache.FieldMeta;
import com.dev.lib.entity.dsl.core.LogicComposer;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.mongo.MongoEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

            Map<String, Predicate> predicateByField = collectPredicates(pathBuilder, fields);
            if (query.where().hasLogic()) {
                Predicate arranged = buildByLogic(query.where().logicTokens(), predicateByField);
                if (arranged != null) {
                    builder.and(arranged);
                }
            } else {
                predicateByField.values().forEach(builder::and);
            }
        }

        for (BooleanExpression expr : expressions) {
            if (expr != null) {
                builder.and(expr);
            }
        }

        return builder;
    }

    private static <E extends MongoEntity> Map<String, Predicate> collectPredicates(
            PathBuilder<E> pathBuilder,
            Collection<QueryFieldMerger.FieldMetaValue> fields
    ) {

        Map<String, Predicate> predicates = new LinkedHashMap<>();

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
                        predicates.put(fm.field().getName(), expr);
                    }
                }

                case GROUP -> {
                    // GROUP 元数据不参与构建，静默忽略
                }

                case SUB_QUERY -> {
                    // MongoDB 嵌入文档查询（替代 JPA 子查询）
                    Predicate embeddedPred = buildEmbeddedPredicate(
                            pathBuilder,
                            fm,
                            value
                    );
                    if (embeddedPred != null) {
                        predicates.put(fm.field().getName(), embeddedPred);
                    }
                }
            }
        }
        return predicates;
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

    private static Predicate buildByLogic(List<QueryWhere.LogicToken> logicTokens, Map<String, Predicate> predicateByField) {

        return LogicComposer.compose(logicTokens, predicateByField::get, new LogicComposer.Combiner<>() {
            @Override
            public Predicate and(Predicate left, Predicate right) {

                return ((BooleanExpression) left).and((BooleanExpression) right);
            }

            @Override
            public Predicate or(Predicate left, Predicate right) {

                return ((BooleanExpression) left).or((BooleanExpression) right);
            }
        });
    }

}
