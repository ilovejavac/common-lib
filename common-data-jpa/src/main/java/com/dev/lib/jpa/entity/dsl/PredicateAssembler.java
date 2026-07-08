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
                boolean hasLogic = query.where().hasLogic();
                Map<String, Predicate> predicateByField = collectPredicates(
                        pathBuilder,
                        fields,
                        !hasLogic
                );
                if (hasLogic) {
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
    static Map<String, Predicate> collectPredicates(
            PathBuilder<?> pathBuilder,
            Collection<QueryFieldMerger.FieldMetaValue> fields,
            boolean mergeBetween
    ) {

        Map<String, Predicate> predicates = new LinkedHashMap<>();
        Map<String, RangeBounds> betweenBounds = mergeBetween
                                                 ? collectBetweenBounds(fields)
                                                 : Map.of();

        for (QueryFieldMerger.FieldMetaValue fv : fields) {
            FieldMeta fm    = fv.getFieldMeta();
            Object    value = fv.getValue();

            if (value == null) continue;

            switch (fm.metaType()) {
                case CONDITION -> {
                    RangeBounds range = betweenBounds.get(fm.targetField());
                    BooleanExpression expr;
                    if (range != null && range.contains(fv)) {
                        if (!range.isLower(fv)) {
                            continue;
                        }
                        expr = ExpressionBuilder.between(
                                pathBuilder,
                                fm.targetFieldParts(),
                                range.lowerValue(),
                                range.upperValue()
                        );
                    } else {
                        expr = ExpressionBuilder.build(
                                pathBuilder,
                                fm.targetFieldParts(),
                                Optional.ofNullable(fm.queryType()).orElse(QueryType.EQ),
                                value
                        );
                    }
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

    private static Map<String, RangeBounds> collectBetweenBounds(
            Collection<QueryFieldMerger.FieldMetaValue> fields
    ) {

        Map<String, RangeBounds> ranges = new LinkedHashMap<>();
        for (QueryFieldMerger.FieldMetaValue fv : fields) {
            FieldMeta fm = fv.getFieldMeta();
            if (!isBetweenMergeCandidate(fm, fv.getValue())) {
                continue;
            }
            ranges.computeIfAbsent(
                    fm.targetField(),
                    ignored -> new RangeBounds()
            ).accept(fv);
        }
        ranges.entrySet().removeIf(entry -> !entry.getValue().canMerge());
        return ranges;
    }

    private static boolean isBetweenMergeCandidate(FieldMeta fm, Object value) {

        if (fm.metaType() != FieldMetaCache.FieldMetaType.CONDITION || !(value instanceof Comparable<?>)) {
            return false;
        }
        QueryType type = Optional.ofNullable(fm.queryType()).orElse(QueryType.EQ);
        return type == QueryType.GE || type == QueryType.LE;
    }

    private static final class RangeBounds {

        private QueryFieldMerger.FieldMetaValue lower;

        private QueryFieldMerger.FieldMetaValue upper;

        private boolean duplicate;

        private void accept(QueryFieldMerger.FieldMetaValue field) {

            QueryType type = field.getFieldMeta().queryType();
            if (type == QueryType.GE) {
                if (lower != null) {
                    duplicate = true;
                }
                lower = field;
            } else if (type == QueryType.LE) {
                if (upper != null) {
                    duplicate = true;
                }
                upper = field;
            }
        }

        private boolean canMerge() {

            return !duplicate
                    && lower != null
                    && upper != null
                    && lowerValue().getClass().isInstance(upperValue());
        }

        private boolean contains(QueryFieldMerger.FieldMetaValue field) {

            return lower == field || upper == field;
        }

        private boolean isLower(QueryFieldMerger.FieldMetaValue field) {

            return lower == field;
        }

        private Object lowerValue() {

            return lower.getValue();
        }

        private Object upperValue() {

            return upper.getValue();
        }

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
