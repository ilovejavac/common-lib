package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryWhere;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.FieldMetaCache.FieldMeta;
import com.dev.lib.entity.dsl.core.LogicComposer;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PredicateAssembler {

    private PredicateAssembler() {

    }

    private static final Map<Class<?>, PathBuilder<?>> PATH_BUILDER_CACHE = new ConcurrentHashMap<>(128);

    @SuppressWarnings("unchecked")
    private static <E extends JpaEntity> PathBuilder<E> getPathBuilder(Class<?> queryClass) {

        return (PathBuilder<E>) PATH_BUILDER_CACHE.computeIfAbsent(queryClass, clazz -> {
            EntityPathBase<?> entityPath = EntityPathManager.getEntityPath(
                    FieldMetaCache.getMeta(clazz).entityClass()
            );
            return new PathBuilder<>(entityPath.getType(), entityPath.getMetadata());
        });
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
            PathBuilder<E> pathBuilder = getPathBuilder(query.getClass());

            if (!CollectionUtils.isEmpty(fields)) {
                Map<String, Predicate> predicateByField = collectPredicates(pathBuilder, fields);
                if (query.where().hasLogic()) {
                    Predicate arranged = buildByLogic(
                            query.where().logicTokens(),
                            predicateByField
                    );
                    Optional.ofNullable(arranged).ifPresent(builder::and);
                } else {
                    predicateByField.values().forEach(builder::and);
                }
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
    private static <E extends JpaEntity> Map<String, Predicate> collectPredicates(
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
                            fm.targetFieldParts(),
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
                    BooleanExpression subExpr = SubQueryBuilder.build(pathBuilder, fm, value);
                    if (subExpr != null) {
                        predicates.put(fm.field().getName(), subExpr);
                    }
                }
            }
        }

        return predicates;
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
