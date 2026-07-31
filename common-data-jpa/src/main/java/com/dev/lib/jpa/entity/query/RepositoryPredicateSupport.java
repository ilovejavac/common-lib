package com.dev.lib.jpa.entity.query;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.DslQueryFieldResolver;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.jpa.entity.dsl.plugin.QueryPluginChain;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.NumberPath;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public final class RepositoryPredicateSupport {

    private RepositoryPredicateSupport() {
    }

    public static <T extends JpaEntity> Predicate buildPredicate(
            PathBuilder<T> pathBuilder,
            EntityPath<T> path,
            NumberPath<Long> deletedPath,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        BooleanBuilder builder = new BooleanBuilder(deletedPath.eq(0L));

        Predicate scopedPredicate = buildPluginAndBusinessPredicate(pathBuilder, path, dslQuery, expressions);
        if (scopedPredicate != null) {
            builder.and(scopedPredicate);
        }
        return builder.getValue();
    }

    public static <T extends JpaEntity> Predicate buildPluginAndBusinessPredicate(
            PathBuilder<T> pathBuilder,
            EntityPath<T> path,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        BooleanBuilder builder = new BooleanBuilder();

        BooleanExpression pluginExpr = QueryPluginChain.getInstance().apply(pathBuilder, path.getType());
        if (pluginExpr != null) {
            builder.and(pluginExpr);
        }

        Predicate dsl = toPredicate(dslQuery, expressions);
        if (dsl != null) {
            builder.and(dsl);
        }
        return builder.getValue();
    }

    public static <T extends JpaEntity> Predicate toPredicate(DslQuery<T> query, BooleanExpression... expressions) {

        BooleanExpression[] safeExpressions = expressions == null
                ? new BooleanExpression[0]
                : expressions;
        if (query != null) {
            Collection<QueryFieldMerger.FieldMetaValue> merged = DslQueryFieldResolver.resolveMerged(
                    query,
                    DslQueryFieldResolver.OverridePolicy.SELF_OVERRIDE_EXTERNAL
            );
            return PredicateAssembler.assemble(query, merged, safeExpressions);
        }
        return safeExpressions.length == 0
                ? null
                : PredicateAssembler.assemble(null, null, safeExpressions);
    }

    public static boolean isEmptyPredicate(Predicate predicate) {

        if (predicate == null) {
            return true;
        }
        if (predicate instanceof BooleanBuilder builder) {
            return !builder.hasValue();
        }
        return false;
    }

    public static Set<String> getAllowFields(DslQuery<?> dslQuery) {

        if (dslQuery == null) {
            return Collections.emptySet();
        }
        return FieldMetaCache.getMeta(dslQuery.getClass()).entityFieldNames();
    }
}
