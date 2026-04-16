package com.dev.lib.jpa.entity.query;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.QueryContext;
import com.dev.lib.jpa.entity.dsl.SelectBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class QueryReadSupport {

    private QueryReadSupport() {
    }

    public static <T extends JpaEntity, D> Optional<D> load(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        if (select == null && resultClass == repository.getEntityClass()) {
            @SuppressWarnings("unchecked")
            Optional<D> fullEntity = (Optional<D>) loadFullEntity(repository, ctx, dslQuery, expressions);
            return fullEntity;
        }

        List<D> results = doQuery(repository, ctx, select, resultClass, dslQuery, expressions, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    public static <T extends JpaEntity, D> List<D> loads(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        if (select == null && resultClass == repository.getEntityClass()) {
            @SuppressWarnings("unchecked")
            List<D> fullEntities = (List<D>) loadsFullEntity(repository, ctx, dslQuery, expressions);
            return fullEntities;
        }

        return doQuery(repository, ctx, select, resultClass, dslQuery, expressions, null);
    }

    public static <T extends JpaEntity, D> Page<D> page(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("分页不支持加锁");
        }

        if (select == null && resultClass == repository.getEntityClass()) {
            @SuppressWarnings("unchecked")
            Page<D> fullEntityPage = (Page<D>) pageFullEntityWithWindowCount(repository, ctx, dslQuery, expressions);
            return fullEntityPage;
        }

        Predicate predicate = buildPredicate(repository, ctx, dslQuery, expressions);

        List<D> content;
        long total;
        try {
            WindowPageResult<D> pageResult = doQueryWithWindowCount(repository, predicate, select, resultClass, dslQuery);
            content = pageResult.content();
            total = pageResult.total();
        } catch (RuntimeException ex) {
            if (!PageQuerySupport.shouldFallbackToLegacyPage(ex)) {
                throw ex;
            }
            JPAQuery<Tuple> fallbackQuery = createTupleQuery(repository, predicate, select.buildExpressions(repository.getPathBuilder()), dslQuery);
            if (dslQuery != null) {
                applyLimit(fallbackQuery, dslQuery);
            }
            content = mapTuples(repository, fallbackQuery.fetch(), select, resultClass);
            total = countByPredicate(repository, predicate);
        }
        return new PageImpl<>(content, resolvePageable(dslQuery), total);
    }

    public static <T extends JpaEntity, D> Stream<D> stream(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("流式查询不支持加锁");
        }

        if (select == null && resultClass == repository.getEntityClass()) {
            @SuppressWarnings("unchecked")
            Stream<D> fullEntityStream = (Stream<D>) streamFullEntity(repository, ctx, dslQuery, expressions);
            return fullEntityStream;
        }

        return doStreamQuery(repository, ctx, select, resultClass, dslQuery, expressions);
    }

    public static <T extends JpaEntity> long count(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        return countByPredicate(repository, buildPredicate(repository, ctx, dslQuery, expressions));
    }

    public static <T extends JpaEntity> boolean exists(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        return repository.getQuerydslExecutor().exists(buildPredicate(repository, ctx, dslQuery, expressions));
    }

    private static <T extends JpaEntity> Predicate buildPredicate(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        return RepositoryPredicateSupport.buildPredicate(
                repository.getPathBuilder(),
                repository.getPath(),
                repository.getDeletedPath(),
                ctx,
                dslQuery,
                expressions
        );
    }

    private static <T extends JpaEntity> long countByPredicate(BaseRepositoryImpl<T> repository, Predicate predicate) {

        return repository.getQuerydslExecutor().count(predicate);
    }

    private static <T extends JpaEntity, D> List<D> doQuery(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery,
            BooleanExpression[] expressions,
            Integer limit
    ) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        JPAQuery<Tuple> query = createTupleQuery(repository, ctx, select.buildExpressions(repository.getPathBuilder()), dslQuery, expressions);

        if (dslQuery != null && limit == null) {
            applyLimit(query, dslQuery);
        }
        if (limit != null) {
            query.limit(limit);
        }

        return mapTuples(repository, query.fetch(), select, resultClass);
    }

    private static <T extends JpaEntity, D> WindowPageResult<D> doQueryWithWindowCount(
            BaseRepositoryImpl<T> repository,
            Predicate predicate,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery
    ) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        Expression<?>[] selectExprs = select.buildExpressions(repository.getPathBuilder());
        Expression<?>[] queryExprs = Arrays.copyOf(selectExprs, selectExprs.length + 1);
        queryExprs[selectExprs.length] = PageQuerySupport.WINDOW_TOTAL_EXPRESSION;

        JPAQuery<Tuple> query = createTupleQuery(repository, predicate, queryExprs, dslQuery);
        if (dslQuery != null) {
            applyLimit(query, dslQuery);
        }

        List<Tuple> tuples = query.fetch();

        if (tuples.isEmpty()) {
            return new WindowPageResult<>(Collections.emptyList(), countByPredicate(repository, predicate));
        }

        long total = PageQuerySupport.resolveWindowTotal(
                tuples,
                PageQuerySupport.WINDOW_TOTAL_EXPRESSION,
                () -> countByPredicate(repository, predicate)
        );
        return new WindowPageResult<>(mapTuples(repository, tuples, select, resultClass), total);
    }

    private static <T extends JpaEntity, D> Stream<D> doStreamQuery(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery,
            BooleanExpression[] expressions
    ) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        JPAQuery<Tuple> query = createTupleQuery(
                repository,
                buildPredicate(repository, ctx, dslQuery, expressions),
                select.buildExpressions(repository.getPathBuilder()),
                dslQuery
        );
        applyStreamFetchSize(query, repository.getJdbcBatchSize());

        return mapTupleStream(repository, query.stream(), select, resultClass);
    }

    private static <T extends JpaEntity> JPAQuery<Tuple> createTupleQuery(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            Expression<?>[] selectExprs,
            DslQuery<T> dslQuery,
            BooleanExpression[] expressions
    ) {

        JPAQuery<Tuple> query = createTupleQuery(repository, buildPredicate(repository, ctx, dslQuery, expressions), selectExprs, dslQuery);
        applyLockOptions(query, ctx);
        return query;
    }

    private static <T extends JpaEntity> JPAQuery<Tuple> createTupleQuery(
            BaseRepositoryImpl<T> repository,
            Predicate predicate,
            Expression<?>[] selectExprs,
            DslQuery<T> dslQuery
    ) {

        JPAQuery<Tuple> query = repository.getQueryFactory().select(selectExprs).from(repository.getPath());

        if (predicate != null) {
            query.where(predicate);
        }
        if (dslQuery != null) {
            applySort(query, repository.getPathBuilder(), dslQuery);
        }

        return query;
    }

    private static <T extends JpaEntity, D> List<D> mapTuples(
            BaseRepositoryImpl<T> repository,
            List<Tuple> tuples,
            SelectBuilder<T> select,
            Class<D> resultClass
    ) {

        if (resultClass == repository.getEntityClass()) {
            @SuppressWarnings("unchecked")
            List<D> entities = (List<D>) tuples.stream().map(select::toEntity).toList();
            return entities;
        }
        return tuples.stream().map(tuple -> select.toDto(tuple, resultClass)).toList();
    }

    private static <T extends JpaEntity, D> Stream<D> mapTupleStream(
            BaseRepositoryImpl<T> repository,
            Stream<Tuple> stream,
            SelectBuilder<T> select,
            Class<D> resultClass
    ) {

        if (resultClass == repository.getEntityClass()) {
            @SuppressWarnings("unchecked")
            Stream<D> entities = (Stream<D>) stream.map(select::toEntity);
            return entities;
        }
        return stream.map(tuple -> select.toDto(tuple, resultClass));
    }

    private static <T extends JpaEntity> Optional<T> loadFullEntity(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        return Optional.ofNullable(
                createEntityQuery(repository, buildPredicate(repository, ctx, dslQuery, expressions), ctx, dslQuery).fetchFirst()
        );
    }

    private static <T extends JpaEntity> List<T> loadsFullEntity(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        Predicate predicate = buildPredicate(repository, ctx, dslQuery, expressions);

        if (ctx.hasLock()) {
            return createEntityQuery(repository, predicate, ctx, dslQuery).fetch();
        }

        if (dslQuery != null && dslQuery.getLimit() != null) {
            return repository.getQuerydslExecutor().findAll(predicate, dslQuery.toPageable(RepositoryPredicateSupport.getAllowFields(dslQuery)))
                    .getContent();
        }

        Sort sort = dslQuery != null ? dslQuery.toSort(RepositoryPredicateSupport.getAllowFields(dslQuery)) : Sort.unsorted();
        return repository.getQuerydslExecutor().findAll(predicate, sort);
    }

    private static <T extends JpaEntity> Page<T> pageFullEntityWithWindowCount(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        Predicate predicate = buildPredicate(repository, ctx, dslQuery, expressions);
        Pageable pageable = resolvePageable(dslQuery);

        try {
            JPAQuery<Tuple> query = repository.getQueryFactory().select(repository.getPath(), PageQuerySupport.WINDOW_TOTAL_EXPRESSION).from(repository.getPath());
            if (predicate != null) {
                query.where(predicate);
            }
            applySort(query, repository.getPathBuilder(), pageable.getSort());
            query.offset(pageable.getOffset());
            query.limit(pageable.getPageSize());

            List<Tuple> tuples = query.fetch();
            if (tuples.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0L);
            }
            List<T> content = tuples.stream()
                    .map(tuple -> tuple.get(repository.getPath()))
                    .filter(Objects::nonNull)
                    .toList();
            long total = PageQuerySupport.resolveWindowTotalFromTuples(tuples, () -> countByPredicate(repository, predicate));
            return new PageImpl<>(content, pageable, total);
        } catch (RuntimeException ex) {
            if (!PageQuerySupport.shouldFallbackToLegacyPage(ex)) {
                throw ex;
            }
            return repository.getQuerydslExecutor().findAll(predicate, pageable);
        }
    }

    private static <T extends JpaEntity> Stream<T> streamFullEntity(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        Predicate predicate = buildPredicate(repository, ctx, dslQuery, expressions);

        JPAQuery<T> query = repository.getQueryFactory().selectFrom(repository.getPath()).where(predicate);
        applyStreamFetchSize(query, repository.getJdbcBatchSize());

        if (dslQuery != null) {
            applySort(query, repository.getPathBuilder(), dslQuery);
        }

        return query.stream();
    }

    private static <T extends JpaEntity> JPAQuery<T> createEntityQuery(
            BaseRepositoryImpl<T> repository,
            Predicate predicate,
            QueryContext ctx,
            DslQuery<T> dslQuery
    ) {

        JPAQuery<T> query = repository.getQueryFactory().selectFrom(repository.getPath()).where(predicate);
        applyLockOptions(query, ctx);
        if (dslQuery != null) {
            applySort(query, repository.getPathBuilder(), dslQuery);
            applyLimit(query, dslQuery);
        }

        return query;
    }

    private static <T extends JpaEntity> void applySort(JPAQuery<?> query, com.querydsl.core.types.dsl.PathBuilder<T> pathBuilder, DslQuery<T> dslQuery) {

        Sort sort = dslQuery.toSort(RepositoryPredicateSupport.getAllowFields(dslQuery));
        applySort(query, pathBuilder, sort);
    }

    private static <T extends JpaEntity> void applySort(JPAQuery<?> query, com.querydsl.core.types.dsl.PathBuilder<T> pathBuilder, Sort sort) {

        if (sort.isUnsorted()) {
            return;
        }

        for (Sort.Order order : sort) {
            query.orderBy(new OrderSpecifier<>(
                    order.isAscending() ? Order.ASC : Order.DESC,
                    pathBuilder.getComparable(order.getProperty(), Comparable.class)
            ));
        }
    }

    private static void applyLimit(JPAQuery<?> query, DslQuery<?> dslQuery) {

        if (dslQuery.getLimit() != null) {
            query.limit(dslQuery.getLimit());
        }
        if (dslQuery.getOffset() != null) {
            query.offset(dslQuery.getOffset());
        }
    }

    private static void applyLockOptions(JPAQuery<?> query, QueryContext ctx) {

        if (!ctx.hasLock()) {
            return;
        }
        query.setLockMode(ctx.getLockMode());
        if (ctx.isSkipLocked()) {
            query.setHint("org.hibernate.lockMode", "UPGRADE_SKIPLOCKED");
        }
    }

    private static void applyStreamFetchSize(JPAQuery<?> query, int jdbcBatchSize) {

        query.setHint(HibernateHints.HINT_FETCH_SIZE, jdbcBatchSize * 2);
    }

    private static Pageable resolvePageable(DslQuery<?> dslQuery) {

        if (dslQuery == null) {
            return PageRequest.of(0, 128, Sort.by(Sort.Order.desc("id")));
        }
        Set<String> allowFields = RepositoryPredicateSupport.getAllowFields(dslQuery);
        return dslQuery.toPageable(allowFields);
    }

    private record WindowPageResult<D>(List<D> content, long total) {
    }
}
