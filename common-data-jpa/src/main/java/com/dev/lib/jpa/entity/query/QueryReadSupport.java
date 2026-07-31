package com.dev.lib.jpa.entity.query;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.QueryContext;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public final class QueryReadSupport {

    private static final int DEFAULT_PAGE_SIZE = 512;

    private static final int MAX_FIND_ALL_SIZE = 10240;

    private QueryReadSupport() {
    }

    public static <T extends JpaEntity> Optional<T> load(
            BaseRepositoryImpl<T> repository,
            QueryContext context,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        Predicate predicate = buildPredicate(repository, context, dslQuery, expressions);
        JPAQuery<T> query = createEntityQuery(repository, predicate, context, dslQuery);
        applyDirectOffset(query, dslQuery);
        return Optional.ofNullable(query.fetchFirst());
    }

    public static <T extends JpaEntity> List<T> loads(
            BaseRepositoryImpl<T> repository,
            QueryContext context,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        Predicate predicate = buildPredicate(repository, context, dslQuery, expressions);
        JPAQuery<T> query = createEntityQuery(repository, predicate, context, dslQuery);
        Integer limit = applyLoadsBounds(query, dslQuery, expressions);
        if (limit == null) {
            return query.fetch();
        }

        List<T> results = query.limit(limit + 1L).fetch();
        if (results.size() > limit) {
            log.warn(
                    "loads result reached its limit: entity={}, size={}, limit={}. Use page or narrower conditions to continue loading remaining rows.",
                    repository.getEntityClass().getName(),
                    results.size(),
                    limit
            );
            return results.subList(0, limit);
        }
        return results;
    }

    public static <T extends JpaEntity> Page<T> page(
            BaseRepositoryImpl<T> repository,
            QueryContext context,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        rejectLock(context, "page");
        Predicate predicate = buildPredicate(repository, context, dslQuery, expressions);
        Pageable pageable = resolvePageable(dslQuery);

        try {
            JPAQuery<Tuple> query = repository.getQueryFactory()
                    .select(repository.getPath(), PageQuerySupport.WINDOW_TOTAL_EXPRESSION)
                    .from(repository.getPath());
            applyPredicate(query, predicate);
            applySort(query, repository.getPathBuilder(), pageable.getSort());
            query.offset(pageable.getOffset());
            query.limit(pageable.getPageSize());

            List<Tuple> tuples = query.fetch();
            if (tuples.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, countByPredicate(repository, predicate));
            }

            List<T> content = tuples.stream()
                    .map(tuple -> tuple.get(repository.getPath()))
                    .filter(Objects::nonNull)
                    .toList();
            long total = PageQuerySupport.resolveWindowTotalFromTuples(
                    tuples,
                    () -> countByPredicate(repository, predicate)
            );
            return new PageImpl<>(content, pageable, total);
        } catch (RuntimeException exception) {
            if (!PageQuerySupport.shouldFallbackToLegacyPage(exception)) {
                throw exception;
            }

            JPAQuery<T> dataQuery = repository.getQueryFactory().selectFrom(repository.getPath());
            applyPredicate(dataQuery, predicate);
            applySort(dataQuery, repository.getPathBuilder(), pageable.getSort());
            dataQuery.offset(pageable.getOffset());
            dataQuery.limit(pageable.getPageSize());
            return new PageImpl<>(dataQuery.fetch(), pageable, countByPredicate(repository, predicate));
        }
    }

    public static <T extends JpaEntity> Stream<T> stream(
            BaseRepositoryImpl<T> repository,
            QueryContext context,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        rejectLock(context, "stream");
        Predicate predicate = buildPredicate(repository, context, dslQuery, expressions);
        JPAQuery<T> query = createEntityQuery(repository, predicate, context, dslQuery);
        query.setHint(HibernateHints.HINT_FETCH_SIZE, repository.getJdbcBatchSize() * 2);
        if (dslQuery != null && dslQuery.getLimit() != null) {
            query.limit(dslQuery.getLimit());
        }
        applyDirectOffset(query, dslQuery);
        return query.stream();
    }

    public static <T extends JpaEntity> long count(
            BaseRepositoryImpl<T> repository,
            QueryContext context,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        return countByPredicate(repository, buildPredicate(repository, context, dslQuery, expressions));
    }

    public static <T extends JpaEntity> boolean exists(
            BaseRepositoryImpl<T> repository,
            QueryContext context,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        Predicate predicate = buildPredicate(repository, context, dslQuery, expressions);
        JPAQuery<Integer> query = repository.getQueryFactory()
                .selectOne()
                .from(repository.getPath());
        applyPredicate(query, predicate);
        return query.fetchFirst() != null;
    }

    private static <T extends JpaEntity> Predicate buildPredicate(
            BaseRepositoryImpl<T> repository,
            QueryContext context,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        return RepositoryPredicateSupport.buildPredicate(
                repository.getPathBuilder(),
                repository.getPath(),
                repository.getDeletedPath(),
                context,
                dslQuery,
                expressions
        );
    }

    private static <T extends JpaEntity> JPAQuery<T> createEntityQuery(
            BaseRepositoryImpl<T> repository,
            Predicate predicate,
            QueryContext context,
            DslQuery<T> dslQuery
    ) {

        JPAQuery<T> query = repository.getQueryFactory().selectFrom(repository.getPath());
        applyPredicate(query, predicate);
        applyLockMode(query, context.getLockMode());
        if (dslQuery != null) {
            applySort(query, repository.getPathBuilder(), dslQuery.toSort(
                    RepositoryPredicateSupport.getAllowFields(dslQuery)
            ));
        }
        return query;
    }

    private static void applyPredicate(JPAQuery<?> query, Predicate predicate) {

        if (predicate != null) {
            query.where(predicate);
        }
    }

    private static <T extends JpaEntity> long countByPredicate(BaseRepositoryImpl<T> repository, Predicate predicate) {

        JPAQuery<Long> query = repository.getQueryFactory()
                .select(repository.getIdPath().count())
                .from(repository.getPath());
        applyPredicate(query, predicate);
        return Optional.ofNullable(query.fetchOne()).orElse(0L);
    }

    private static <T extends JpaEntity> Integer applyLoadsBounds(
            JPAQuery<?> query,
            DslQuery<T> dslQuery,
            BooleanExpression[] expressions
    ) {

        Integer requestedLimit = dslQuery == null ? null : dslQuery.getLimit();
        Integer limit;
        if (hasUsableCondition(dslQuery, expressions)) {
            // 有可用过滤条件：显式 limit 按 limit，否则返回全部匹配数据
            limit = requestedLimit;
        } else if (requestedLimit == null) {
            // 无条件等价 find-all，用上限保护，防止误拉全表
            limit = MAX_FIND_ALL_SIZE;
        } else {
            // find-all 即使设置了 limit，也以 MAX_FIND_ALL_SIZE 封顶
            limit = Math.min(requestedLimit, MAX_FIND_ALL_SIZE);
        }
        if (limit != null) {
            query.limit(limit);
        }
        applyDirectOffset(query, dslQuery);
        return limit;
    }

    private static <T extends JpaEntity> boolean hasUsableCondition(
            DslQuery<T> dslQuery,
            BooleanExpression[] expressions
    ) {

        return !RepositoryPredicateSupport.isEmptyPredicate(
                RepositoryPredicateSupport.toPredicate(dslQuery, expressions)
        );
    }

    private static void applyDirectOffset(JPAQuery<?> query, DslQuery<?> dslQuery) {

        if (dslQuery != null && dslQuery.getOffset() != null) {
            query.offset(dslQuery.getOffset());
        }
    }

    private static <T extends JpaEntity> void applySort(
            JPAQuery<?> query,
            com.querydsl.core.types.dsl.PathBuilder<T> pathBuilder,
            Sort sort
    ) {

        for (Sort.Order order : sort) {
            query.orderBy(new OrderSpecifier<>(
                    order.isAscending() ? Order.ASC : Order.DESC,
                    pathBuilder.getComparable(order.getProperty(), Comparable.class)
            ));
        }
    }

    private static void applyLockMode(JPAQuery<?> query, jakarta.persistence.LockModeType lockMode) {

        if (lockMode == null) {
            return;
        }
        query.setLockMode(lockMode);
    }

    private static void rejectLock(QueryContext context, String operation) {

        if (context.hasLock()) {
            throw new IllegalStateException(operation + " 不支持锁查询，锁仅适用于 load/loads");
        }
    }

    private static Pageable resolvePageable(DslQuery<?> dslQuery) {

        if (dslQuery == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Order.desc("id")));
        }
        Set<String> allowFields = RepositoryPredicateSupport.getAllowFields(dslQuery);
        return dslQuery.toPageable(allowFields);
    }
}
