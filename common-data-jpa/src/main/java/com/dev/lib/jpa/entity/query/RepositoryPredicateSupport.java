package com.dev.lib.jpa.entity.query;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.DslQueryFieldResolver;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.QueryContext;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.jpa.entity.dsl.plugin.QueryPluginChain;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.PathBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public final class RepositoryPredicateSupport {

    private RepositoryPredicateSupport() {
    }

    public static <T extends JpaEntity> Predicate buildPredicate(
            PathBuilder<T> pathBuilder,
            EntityPath<T> path,
            BooleanPath deletedPath,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        BooleanBuilder builder = new BooleanBuilder();

        switch (ctx.getDeletedFilter()) {
            case EXCLUDE_DELETED -> builder.and(deletedPath.eq(false));
            case ONLY_DELETED -> builder.and(deletedPath.eq(true));
        }

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

        if (query != null) {
            Collection<QueryFieldMerger.FieldMetaValue> merged = DslQueryFieldResolver.resolveMerged(
                    query,
                    DslQueryFieldResolver.OverridePolicy.SELF_OVERRIDE_EXTERNAL
            );
            return PredicateAssembler.assemble(query, merged, expressions);
        }
        return expressions.length == 0 ? null : PredicateAssembler.assemble(null, null, expressions);
    }

    public static boolean isEmptyPredicate(Predicate predicate) {

        if (predicate == null) {
            return true;
        }
        if (predicate instanceof BooleanBuilder bb) {
            return !bb.hasValue();
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
