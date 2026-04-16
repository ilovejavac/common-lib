package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.agg.AggregateSpec;
import com.dev.lib.jpa.entity.aggregate.AggregateExecutor;
import com.dev.lib.jpa.entity.batch.BatchOperationSupport;
import com.dev.lib.jpa.entity.delete.CascadeSoftDeleteSupport;
import com.dev.lib.jpa.entity.query.PageQuerySupport;
import com.dev.lib.jpa.entity.query.QueryReadSupport;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.dev.lib.jpa.entity.dsl.SelectBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

@Getter
public class BaseRepositoryImpl<T extends JpaEntity> extends SimpleJpaRepository<T, Long> implements BaseRepository<T> {

    private static final int DEFAULT_JDBC_BATCH_SIZE = 256;

    private static final int DEFAULT_IN_CLAUSE_BATCH_SIZE = 1000;

    private static final String JDBC_BATCH_SIZE_PROPERTY = "hibernate.jdbc.batch_size";

    private static final String IN_CLAUSE_BATCH_SIZE_PROPERTY = "app.jpa.in-clause-batch-size";

    private final EntityManagerFactory entityManagerFactory;

    private final EntityManager entityManager;

    private final QuerydslJpaPredicateExecutor<T> querydslExecutor;

    private final Class<T> entityClass;

    private final JPAQueryFactory queryFactory;

    private final EntityPath<T> path;

    private final PathBuilder<T> pathBuilder;

    private final BooleanPath deletedPath;

    private final NumberPath<Long> idPath;

    private final int jdbcBatchSize;

    private final int inClauseBatchSize;

    public BaseRepositoryImpl(JpaEntityInformation<T, Long> entityInformation, EntityManager em) {

        super(entityInformation, em);
        this.entityManager = em;
        this.entityManagerFactory = em.getEntityManagerFactory();
        this.entityClass = entityInformation.getJavaType();

        this.path = SimpleEntityPathResolver.INSTANCE.createPath(entityClass);
        this.pathBuilder = new PathBuilder<>(path.getType(), path.getMetadata());
        this.deletedPath = resolvePath(path, "deleted", BooleanPath.class);
        this.idPath = pathBuilder.getNumber("id", Long.class);

        this.jdbcBatchSize = resolveConfiguredBatchSize(em, JDBC_BATCH_SIZE_PROPERTY, DEFAULT_JDBC_BATCH_SIZE);
        this.inClauseBatchSize = resolveConfiguredBatchSize(em, IN_CLAUSE_BATCH_SIZE_PROPERTY, DEFAULT_IN_CLAUSE_BATCH_SIZE);

        this.querydslExecutor = new QuerydslJpaPredicateExecutor<>(
                entityInformation,
                em,
                SimpleEntityPathResolver.INSTANCE,
                null
        );

        this.queryFactory = new JPAQueryFactory(em);

    }

    @Override
    public T ref(Long id) {

        return entityManager.getReference(entityClass, id);
    }

    @Override
    public Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return load(new QueryContext(), null, entityClass, dslQuery, expressions);
    }

    @Override
    public List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return loads(new QueryContext(), null, entityClass, dslQuery, expressions);
    }

    @Override
    public Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return page(new QueryContext(), null, entityClass, dslQuery, expressions);
    }

    @Override
    public Stream<T> stream(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return stream(new QueryContext(), null, entityClass, dslQuery, expressions);
    }

    @Override
    public boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return exists(new QueryContext(), dslQuery, expressions);
    }

    @Override
    public long count(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return count(new QueryContext(), dslQuery, expressions);
    }

    @Override
    public <R> List<R> aggregate(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        if (dslQuery == null) {
            throw new IllegalArgumentException("聚合查询不能为空");
        }
        AggregateSpec<T, R> spec = dslQuery.aggregateSpec();
        if (spec == null || spec.isEmpty()) {
            throw new IllegalArgumentException("聚合查询缺少 agg() 映射配置");
        }

        Predicate predicate = RepositoryPredicateSupport.buildPredicate(
                pathBuilder,
                path,
                deletedPath,
                new QueryContext(),
                dslQuery,
                expressions
        );
        return AggregateExecutor.executeAggregateQuery(entityManager, path, pathBuilder, predicate, spec);
    }

    <D> Optional<D> load(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "load");
        return QueryReadSupport.load(this, ctx, select, resultClass, dslQuery, expressions);
    }

    <D> List<D> loads(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "loads");
        return QueryReadSupport.loads(this, ctx, select, resultClass, dslQuery, expressions);
    }

    <D> Page<D> page(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "page");
        return QueryReadSupport.page(this, ctx, select, resultClass, dslQuery, expressions);
    }

    <D> Stream<D> stream(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "stream");
        return QueryReadSupport.stream(this, ctx, select, resultClass, dslQuery, expressions);
    }

    long count(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "count");
        return QueryReadSupport.count(this, ctx, dslQuery, expressions);
    }

    boolean exists(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "exists");
        return QueryReadSupport.exists(this, ctx, dslQuery, expressions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <S extends T> @NonNull List<S> saveAll(@NonNull Iterable<S> entities) {

        return BatchOperationSupport.saveAll(this, entities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        delete(new QueryContext(), dslQuery, expressions);
    }

    void delete(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "delete");
        CascadeSoftDeleteSupport.delete(this, ctx, dslQuery, expressions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(@NonNull T entity) {

        CascadeSoftDeleteSupport.deleteEntity(this, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(@NonNull Long id) {

        CascadeSoftDeleteSupport.deleteById(this, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(@NonNull Iterable<? extends T> entities) {

        CascadeSoftDeleteSupport.deleteAll(this, entities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllById(@NonNull Iterable<? extends Long> ids) {

        CascadeSoftDeleteSupport.deleteAllById(this, ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll() {

        CascadeSoftDeleteSupport.deleteAll(this);
    }

    void hardDelete(T entity) {

        BatchOperationSupport.hardDelete(this, entity);
    }

    void hardDeleteById(Long id) {

        BatchOperationSupport.hardDeleteById(this, id);
    }

    void hardDeleteAll(Iterable<? extends T> entities) {

        BatchOperationSupport.hardDeleteAll(this, entities);
    }

    void hardDeleteAllById(Iterable<Long> ids) {

        BatchOperationSupport.hardDeleteAllById(this, ids);
    }

    long hardDelete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "hardDelete");
        return BatchOperationSupport.hardDelete(this, dslQuery, expressions);
    }

    static long resolveWindowTotal(List<Tuple> tuples, Expression<Long> totalExpression, LongSupplier countFallback) {

        return PageQuerySupport.resolveWindowTotal(tuples, totalExpression, countFallback);
    }

    static long resolveWindowTotalFromTuples(List<Tuple> tuples, LongSupplier countFallback) {

        return PageQuerySupport.resolveWindowTotalFromTuples(tuples, countFallback);
    }

    static boolean shouldFallbackToLegacyPage(Throwable ex) {

        return PageQuerySupport.shouldFallbackToLegacyPage(ex);
    }

    private int resolveConfiguredBatchSize(EntityManager em, String propertyKey, int defaultValue) {

        Object configured = em.getEntityManagerFactory().getProperties().get(propertyKey);
        return normalizeBatchSize(configured, defaultValue);
    }

    private int normalizeBatchSize(Object configured, int defaultValue) {

        if (configured == null) {
            return defaultValue;
        }

        int value;
        if (configured instanceof Number number) {
            value = number.intValue();
        } else {
            try {
                value = Integer.parseInt(configured.toString());
            } catch (Exception ignored) {
                value = defaultValue;
            }
        }
        return Math.max(1, value);
    }

    @SuppressWarnings("unchecked")
    private <P> P resolvePath(EntityPath<T> currentPath, String fieldName, Class<P> type) {

        try {
            return (P) currentPath.getClass().getField(fieldName).get(currentPath);
        } catch (Exception e) {
            throw new IllegalStateException("实体缺少 " + fieldName + " 字段", e);
        }
    }

    private void ensureNonAggregateQuery(DslQuery<T> dslQuery, String operation) {

        if (dslQuery != null && dslQuery.hasAgg()) {
            throw new IllegalStateException("检测到 agg() 聚合配置，" + operation + " 不支持聚合查询，请使用 aggregate()");
        }
    }

    @Override
    public @NonNull Optional<T> findOne(@NonNull Predicate predicate) {

        return querydslExecutor.findOne(predicate);
    }

    @Override
    public @NonNull List<T> findAll(@NonNull Predicate predicate) {

        return querydslExecutor.findAll(predicate);
    }

    @Override
    public @NonNull List<T> findAll(@NonNull Predicate predicate, @NonNull Sort sort) {

        return querydslExecutor.findAll(predicate, sort);
    }

    @Override
    public @NonNull List<T> findAll(@NonNull Predicate predicate, @NonNull OrderSpecifier<?>... orders) {

        return querydslExecutor.findAll(predicate, orders);
    }

    @Override
    public @NonNull List<T> findAll(OrderSpecifier<?> @NonNull ... orders) {

        return querydslExecutor.findAll(orders);
    }

    @Override
    public @NonNull Page<T> findAll(@NonNull Predicate predicate, @NonNull Pageable pageable) {

        return querydslExecutor.findAll(predicate, pageable);
    }

    @Override
    public long count(@NonNull Predicate predicate) {

        return querydslExecutor.count(predicate);
    }

    @Override
    public boolean exists(@NonNull Predicate predicate) {

        return querydslExecutor.exists(predicate);
    }

    @Override
    public <S extends T, R> @NonNull R findBy(
            @NonNull Predicate predicate,
            @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction
    ) {

        return querydslExecutor.findBy(predicate, queryFunction);
    }
}
