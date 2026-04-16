package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.agg.AggType;
import com.dev.lib.entity.dsl.agg.AggregateSpec;
import com.dev.lib.entity.dsl.core.DslQueryFieldResolver;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.jpa.entity.dsl.SelectBuilder;
import com.dev.lib.jpa.entity.dsl.plugin.QueryPluginChain;
import com.dev.lib.security.util.SecurityContextHolder;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.jpa.HibernateHints;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

public class BaseRepositoryImpl<T extends JpaEntity> extends SimpleJpaRepository<T, Long> implements BaseRepository<T> {

    private static final int DEFAULT_JDBC_BATCH_SIZE = 256;

    private static final int DEFAULT_IN_CLAUSE_BATCH_SIZE = 1024;

    private static final Expression<Long> WINDOW_TOTAL_EXPRESSION = Expressions.numberTemplate(
            Long.class,
            "count(*) over()"
    ).as("__total__");

    private static final Map<Class<?>, List<Field>> CASCADE_FIELDS_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, Map<String, Field>> AGG_TARGET_FIELD_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, Constructor<?>> AGG_TARGET_CTOR_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, Map<String, Method>> AGG_TARGET_SETTER_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, AggregateCtorPlan> AGG_TARGET_CTOR_PLAN_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, PathBuilder<?>> PATH_BUILDER_CACHE = new ConcurrentHashMap<>(128);

    private final EntityManager entityManager;

    private final JPAQueryFactory queryFactory;

    private final EntityPath<T> path;

    private final PathBuilder<T> pathBuilder;

    private final QuerydslJpaPredicateExecutor<T> querydslExecutor;

    private final BooleanPath deletedPath;

    private final NumberPath<Long> idPath;

    private final Class<T> entityClass;

    private final boolean hasCascadeFields;

    private final int jdbcBatchSize;

    private final int inClauseBatchSize;

    public BaseRepositoryImpl(JpaEntityInformation<T, Long> entityInformation, EntityManager em) {

        super(entityInformation, em);
        this.entityManager = em;
        this.queryFactory = new JPAQueryFactory(em);
        this.path = SimpleEntityPathResolver.INSTANCE.createPath(entityInformation.getJavaType());
        this.pathBuilder = new PathBuilder<>(path.getType(), path.getMetadata());
        this.entityClass = entityInformation.getJavaType();
        this.querydslExecutor = new QuerydslJpaPredicateExecutor<>(
                entityInformation, em, SimpleEntityPathResolver.INSTANCE, null
        );
        this.deletedPath = resolvePath("deleted", BooleanPath.class);
        this.idPath = pathBuilder.getNumber("id", Long.class);
        this.hasCascadeFields = !getCascadeFields(path.getType()).isEmpty();
        this.jdbcBatchSize = resolveConfiguredJdbcBatchSize(em);
        this.inClauseBatchSize = DEFAULT_IN_CLAUSE_BATCH_SIZE;
    }

    private int resolveConfiguredJdbcBatchSize(EntityManager em) {

        Object configured = em.getEntityManagerFactory().getProperties().get("hibernate.jdbc.batch_size");
        return normalizeBatchSize(configured, DEFAULT_JDBC_BATCH_SIZE);
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

    // ==================== Getter ====================

    Class<T> getEntityClass() {

        return entityClass;
    }

    PathBuilder<T> getPathBuilder() {

        return pathBuilder;
    }

    EntityManagerFactory getEntityManagerFactory() {

        return entityManager.getEntityManagerFactory();
    }

    // ==================== 公开接口方法（默认上下文）====================

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

        Predicate predicate = buildPredicate(new QueryContext(), dslQuery, expressions);
        return executeAggregateQuery(predicate, spec);
    }

    // ==================== 核心查询方法（统一入口）====================

    @SuppressWarnings("unchecked")
    <D> Optional<D> load(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "load");
        if (select == null && resultClass == entityClass) {
            return (Optional<D>) loadFullEntity(ctx, dslQuery, expressions);
        }

        List<D> results = doQuery(ctx, select, resultClass, dslQuery, expressions, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @SuppressWarnings("unchecked")
    <D> List<D> loads(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "loads");
        if (select == null && resultClass == entityClass) {
            return (List<D>) loadsFullEntity(ctx, dslQuery, expressions);
        }

        return doQuery(ctx, select, resultClass, dslQuery, expressions, null);
    }

    @SuppressWarnings("unchecked")
    <D> Page<D> page(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "page");
        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("分页不支持加锁");
        }

        if (select == null && resultClass == entityClass) {
            return (Page<D>) pageFullEntityWithWindowCount(ctx, dslQuery, expressions);
        }

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);

        List<D> content;
        long    total;
        try {
            WindowPageResult<D> pageResult = doQueryWithWindowCount(predicate, select, resultClass, dslQuery);
            content = pageResult.content();
            total = pageResult.total();
        } catch (RuntimeException ex) {
            if (!shouldFallbackToLegacyPage(ex)) {
                throw ex;
            }
            JPAQuery<Tuple> fallbackQuery = createTupleQuery(predicate, select.buildExpressions(pathBuilder), dslQuery);
            if (dslQuery != null) {
                applyLimit(fallbackQuery, dslQuery);
            }
            content = mapTuples(fallbackQuery.fetch(), select, resultClass);
            total = countByPredicate(predicate);
        }
        return new PageImpl<>(content, dslQuery.toPageable(null), total);
    }

    @SuppressWarnings("unchecked")
    <D> Stream<D> stream(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "stream");
        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("流式查询不支持加锁");
        }

        if (select == null && resultClass == entityClass) {
            return (Stream<D>) streamFullEntity(ctx, dslQuery, expressions);
        }

        return doStreamQuery(ctx, select, resultClass, dslQuery, expressions);
    }

    long count(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "count");
        return countByPredicate(buildPredicate(ctx, dslQuery, expressions));
    }

    private long countByPredicate(Predicate predicate) {

        return querydslExecutor.count(predicate);
    }

    boolean exists(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "exists");
        return querydslExecutor.exists(buildPredicate(ctx, dslQuery, expressions));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <S extends T> List<S> saveAll(Iterable<S> entities) {

        if (entities == null) {
            return Collections.emptyList();
        }

        List<S> result       = new ArrayList<>();
        List<S> managedBatch = new ArrayList<>(jdbcBatchSize);
        for (S entity : entities) {
            if (entity == null) {
                continue;
            }
            S managed;
            if (entity.isNew()) {
                entityManager.persist(entity);
                managed = entity;
            } else {
                managed = entityManager.merge(entity);
            }
            result.add(managed);
            managedBatch.add(managed);
            if (managedBatch.size() >= jdbcBatchSize) {
                flushAndDetach(managedBatch);
            }
        }

        if (!managedBatch.isEmpty()) {
            flushAndDetach(managedBatch);
        }

        return result;
    }

    private void flushAndDetach(List<?> managedEntities) {

        entityManager.flush();
        for (Object entity : managedEntities) {
            if (entity != null && entityManager.contains(entity)) {
                entityManager.detach(entity);
            }
        }
        managedEntities.clear();
    }

    // ==================== 内部查询实现 ====================

    /**
     * 执行部分字段查询（从 ctx 构建 predicate）
     */
    private <D> List<D> doQuery(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression[] expressions, Integer limit) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        JPAQuery<Tuple> query = createTupleQuery(ctx, select.buildExpressions(pathBuilder), dslQuery, expressions);

        if (dslQuery != null && limit == null) {
            applyLimit(query, dslQuery);
        }
        if (limit != null) {
            query.limit(limit);
        }

        return mapTuples(query.fetch(), select, resultClass);
    }

    /**
     * 部分字段分页查询（复用已构建的 predicate）
     */
    private <D> WindowPageResult<D> doQueryWithWindowCount(
            Predicate predicate,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        Expression<?>[] selectExprs = select.buildExpressions(pathBuilder);
        Expression<?>[] queryExprs  = Arrays.copyOf(selectExprs, selectExprs.length + 1);
        queryExprs[selectExprs.length] = WINDOW_TOTAL_EXPRESSION;

        JPAQuery<Tuple> query = createTupleQuery(predicate, queryExprs, dslQuery);
        if (dslQuery != null) {
            applyLimit(query, dslQuery);
        }

        List<Tuple> tuples = query.fetch();

        if (tuples.isEmpty()) {
            return new WindowPageResult<>(Collections.emptyList(), countByPredicate(predicate));
        }

        long total = resolveWindowTotal(tuples, WINDOW_TOTAL_EXPRESSION, () -> countByPredicate(predicate));
        return new WindowPageResult<>(mapTuples(tuples, select, resultClass), total);
    }

    static long resolveWindowTotal(List<Tuple> tuples, Expression<Long> totalExpression, LongSupplier countFallback) {

        if (tuples == null || tuples.isEmpty()) {
            return countFallback.getAsLong();
        }

        Long total = tuples.getFirst().get(totalExpression);
        if (total != null) {
            return total;
        }
        // 数据库不支持 count(*) over()，fallback 到 count 查询
        return countFallback.getAsLong();
    }

    /**
     * 全字段查询时，Hibernate 展开实体列后 Expression 引用匹配不上，
     * 改用 Tuple 最后一列（即 count(*) over()）取值。
     */
    static long resolveWindowTotalFromTuples(List<Tuple> tuples, LongSupplier countFallback) {

        if (tuples == null || tuples.isEmpty()) {
            return countFallback.getAsLong();
        }

        Object[] array = tuples.getFirst().toArray();
        Object   last  = array[array.length - 1];
        if (last instanceof Number num) {
            return num.longValue();
        }
        // 数据库不支持 count(*) over()，fallback 到 count 查询
        return countFallback.getAsLong();
    }

    static boolean shouldFallbackToLegacyPage(Throwable ex) {

        for (Throwable current = ex; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("window")
                    || normalized.contains("over(")
                    || normalized.contains("count(*) over()")
                    || normalized.contains("syntax")
                    || normalized.contains("parse")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行部分字段流式查询
     */
    private <D> Stream<D> doStreamQuery(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression[] expressions) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        JPAQuery<Tuple> query = createTupleQuery(
                buildPredicate(ctx, dslQuery, expressions),
                select.buildExpressions(pathBuilder),
                dslQuery
        );
        applyStreamFetchSize(query);

        return mapTupleStream(query.stream(), select, resultClass);
    }

    private record WindowPageResult<D>(List<D> content, long total) {
    }

    // ==================== Tuple 查询公共构建 ====================

    private JPAQuery<Tuple> createTupleQuery(QueryContext ctx, Expression<?>[] selectExprs, DslQuery<T> dslQuery, BooleanExpression[] expressions) {

        JPAQuery<Tuple> query = createTupleQuery(buildPredicate(ctx, dslQuery, expressions), selectExprs, dslQuery);
        applyLockOptions(query, ctx);
        return query;
    }

    private JPAQuery<Tuple> createTupleQuery(Predicate predicate, Expression<?>[] selectExprs, DslQuery<T> dslQuery) {

        JPAQuery<Tuple> query = queryFactory.select(selectExprs).from(path);

        if (predicate != null) {
            query.where(predicate);
        }
        if (dslQuery != null) {
            applySort(query, dslQuery);
        }

        return query;
    }

    @SuppressWarnings("unchecked")
    private <D> List<D> mapTuples(List<Tuple> tuples, SelectBuilder<T> select, Class<D> resultClass) {

        if (resultClass == entityClass) {
            return (List<D>) tuples.stream().map(select::toEntity).toList();
        }
        return tuples.stream().map(t -> select.toDto(t, resultClass)).toList();
    }

    @SuppressWarnings("unchecked")
    private <D> Stream<D> mapTupleStream(Stream<Tuple> stream, SelectBuilder<T> select, Class<D> resultClass) {

        if (resultClass == entityClass) {
            return (Stream<D>) stream.map(select::toEntity);
        }
        return stream.map(t -> select.toDto(t, resultClass));
    }

    // ==================== 全字段查询（优化路径）====================

    private Optional<T> loadFullEntity(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return Optional.ofNullable(
                createEntityQuery(buildPredicate(ctx, dslQuery, expressions), ctx, dslQuery).fetchFirst()
        );
    }

    private List<T> loadsFullEntity(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);

        if (ctx.hasLock()) {
            return createEntityQuery(predicate, ctx, dslQuery).fetch();
        }

        if (dslQuery != null && dslQuery.getLimit() != null) {
            return querydslExecutor.findAll(predicate, dslQuery.toPageable(getAllowFields(dslQuery)))
                    .getContent();
        }

        Sort sort = dslQuery != null ? dslQuery.toSort(getAllowFields(dslQuery)) : Sort.unsorted();
        return querydslExecutor.findAll(predicate, sort);
    }

    private Page<T> pageFullEntityWithWindowCount(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);
        Pageable  pageable  = dslQuery.toPageable(getAllowFields(dslQuery));

        try {
            JPAQuery<Tuple> query = queryFactory.select(path, WINDOW_TOTAL_EXPRESSION).from(path);
            if (predicate != null) {
                query.where(predicate);
            }
            applySort(query, pageable.getSort());
            query.offset(pageable.getOffset());
            query.limit(pageable.getPageSize());

            List<Tuple> tuples = query.fetch();
            if (tuples.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0L);
            }
            List<T> content = tuples.stream()
                    .map(tuple -> tuple.get(path))
                    .filter(Objects::nonNull)
                    .toList();
            long total = resolveWindowTotalFromTuples(tuples, () -> countByPredicate(predicate));
            return new PageImpl<>(content, pageable, total);
        } catch (RuntimeException ex) {
            if (!shouldFallbackToLegacyPage(ex)) {
                throw ex;
            }
            return querydslExecutor.findAll(predicate, pageable);
        }
    }

    private Stream<T> streamFullEntity(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);

        JPAQuery<T> query = queryFactory.selectFrom(path).where(predicate);
        applyStreamFetchSize(query);

        if (dslQuery != null) {
            applySort(query, dslQuery);
        }

        return query.stream();
    }

    // ==================== 全字段 JPAQuery 构建 ====================

    private JPAQuery<T> createEntityQuery(Predicate predicate, QueryContext ctx, DslQuery<T> dslQuery) {

        JPAQuery<T> query = queryFactory.selectFrom(path).where(predicate);
        applyLockOptions(query, ctx);
        if (dslQuery != null) {
            applySort(query, dslQuery);
            applyLimit(query, dslQuery);
        }

        return query;
    }

    // ==================== 软删除 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        delete(new QueryContext(), dslQuery, expressions);
    }

    void delete(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "delete");
        Predicate businessPredicate = toPredicate(dslQuery, expressions);
        if (isEmptyPredicate(businessPredicate)) {
            throw new IllegalArgumentException("批量删除必须指定业务条件，防止误删全表");
        }

        if (hasCascadeFields) {
            softDeleteByPredicateInChunks(buildPredicate(ctx, dslQuery, expressions));
        } else {
            BooleanBuilder where = new BooleanBuilder();
            if (ctx.getDeletedFilter() == QueryContext.DeletedFilter.EXCLUDE_DELETED) {
                where.and(deletedPath.eq(false));
            }
            where.and(businessPredicate);
            executeSoftDeleteUpdate(where);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(@NonNull T entity) {

        if (entity.getId() == null) return;
        softDeleteById(entity.getId(), entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(@NonNull Long id) {

        softDeleteById(id, null);
    }

    private void softDeleteById(Long id, T entityHint) {

        if (hasCascadeFields) {
            T managed = (entityHint != null && entityManager.contains(entityHint))
                        ? entityHint
                        : entityManager.find(path.getType(), id);
            if (managed != null) {
                cascadeSoftDeleteBatch(List.of(managed));
                return;
            }
        }

        executeSoftDeleteUpdate(idPath.eq(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(@NonNull Iterable<? extends T> entities) {

        forEachBatch(entities, e -> (e != null && e.getId() != null) ? e.getId() : null, this::softDeleteByIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllById(@NonNull Iterable<? extends Long> ids) {

        forEachBatch(ids, id -> id, this::softDeleteByIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll() {

        if (!hasCascadeFields) {
            executeSoftDeleteUpdate(deletedPath.eq(false));
            return;
        }

        Long lastId = null;
        while (true) {
            List<Long> ids = fetchIdsAfter(deletedPath.eq(false), lastId);
            if (ids.isEmpty()) break;
            softDeleteByIds(ids);
            lastId = ids.getLast();
        }
    }

    private void softDeleteByIds(List<Long> ids) {

        if (!hasCascadeFields) {
            executeSoftDeleteUpdate(idPath.in(ids));
            return;
        }

        // 级联场景：只查未删除的，cascadeSoftDeleteBatch 内部会按需加载级联字段
        List<T> toDelete = queryFactory.selectFrom(path)
                .where(idPath.in(ids), deletedPath.eq(false))
                .fetch();
        if (!toDelete.isEmpty()) {
            cascadeSoftDeleteBatch(toDelete);
        }
    }

    private void softDeleteByPredicateInChunks(Predicate fullPredicate) {

        Long lastId = null;
        while (true) {
            List<Long> ids = fetchIdsAfter(fullPredicate, lastId);
            if (ids.isEmpty()) break;

            softDeleteByIds(ids);
            lastId = ids.getLast();
        }
    }

    private void executeSoftDeleteUpdate(Predicate condition) {

        BooleanBuilder where = new BooleanBuilder(deletedPath.eq(false));
        where.and(condition);

        long affected = queryFactory.update(path)
                .set(pathBuilder.getBoolean("deleted"), true)
                .set(pathBuilder.getDateTime("updatedAt", LocalDateTime.class), LocalDateTime.now())
                .set(pathBuilder.getNumber("modifierId", Long.class), SecurityContextHolder.getUserId())
                .where(where)
                .execute();

        if (affected > 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }

    private void cascadeSoftDeleteBatch(List<T> rootEntities) {

        List<T> fullyLoadedEntities = ensureCascadeFieldsLoaded(rootEntities);

        Map<Class<?>, Set<Long>> toDeleteByType = new LinkedHashMap<>();
        Set<Object>              visited        = Collections.newSetFromMap(new IdentityHashMap<>());

        for (T entity : fullyLoadedEntities) {
            collectCascadeEntities(entity, visited, toDeleteByType);
        }

        LocalDateTime now        = LocalDateTime.now();
        Long          modifierId = SecurityContextHolder.getUserId();

        for (Map.Entry<Class<?>, Set<Long>> entry : toDeleteByType.entrySet()) {
            Class<?>   clazz = entry.getKey();
            List<Long> ids   = new ArrayList<>(entry.getValue());
            if (ids.isEmpty()) continue;

            PathBuilder<?>   builder       = createPathBuilder(clazz);
            NumberPath<Long> builderIdPath = builder.getNumber("id", Long.class);

            for (int i = 0; i < ids.size(); i += inClauseBatchSize) {
                List<Long> batch = ids.subList(i, Math.min(i + inClauseBatchSize, ids.size()));
                queryFactory.update(builder)
                        .set(builder.getBoolean("deleted"), true)
                        .set(builder.getDateTime("updatedAt", LocalDateTime.class), now)
                        .set(builder.getNumber("modifierId", Long.class), modifierId)
                        .where(builderIdPath.in(batch))
                        .execute();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    // ==================== 物理删除 ====================

    void hardDelete(T entity) {

        if (entity == null || entity.getId() == null) return;
        hardDeleteById(entity.getId());
    }

    void hardDeleteById(Long id) {

        if (id == null) return;

        queryFactory.delete(path)
                .where(idPath.eq(id))
                .execute();
        flushAndDetachByIds(List.of(id));
    }

    void hardDeleteAll(Iterable<? extends T> entities) {

        if (entities == null) return;
        forEachBatch(entities, e -> (e != null && e.getId() != null) ? e.getId() : null, this::batchHardDeleteByIds);
    }

    void hardDeleteAllById(Iterable<Long> ids) {

        if (ids == null) return;
        forEachBatch(ids, id -> id, this::batchHardDeleteByIds);
    }

    private void batchHardDeleteByIds(List<Long> ids) {

        queryFactory.delete(path)
                .where(idPath.in(ids))
                .execute();

        flushAndDetachByIds(ids);
    }

    long hardDelete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "hardDelete");
        QueryContext ctx           = new QueryContext().withDeleted();
        long         totalAffected = 0;
        Predicate    predicate     = buildPredicate(ctx, dslQuery, expressions);

        Long lastId = null;
        while (true) {
            List<Long> ids = fetchIdsAfter(predicate, lastId);
            if (ids.isEmpty()) break;

            long affected = queryFactory.delete(path)
                    .where(idPath.in(ids))
                    .execute();

            totalAffected += affected;
            flushAndDetachByIds(ids);
            lastId = ids.getLast();
        }

        return totalAffected;
    }

    private void flushAndDetachByIds(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return;
        }
        entityManager.flush();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            T reference = entityManager.getReference(entityClass, id);
            if (entityManager.contains(reference)) {
                entityManager.detach(reference);
            }
        }
    }

    // ==================== 批量迭代工具 ====================

    private List<Long> fetchIdsAfter(Predicate basePredicate, Long lastId) {

        BooleanBuilder where = new BooleanBuilder(basePredicate);
        if (lastId != null) {
            where.and(idPath.gt(lastId));
        }
        return queryFactory.select(idPath)
                .from(path)
                .where(where)
                .orderBy(idPath.asc())
                .limit(inClauseBatchSize)
                .fetch();
    }

    private <E> void forEachBatch(Iterable<? extends E> source, Function<E, Long> idExtractor, java.util.function.Consumer<List<Long>> batchAction) {

        List<Long> batch = new ArrayList<>(inClauseBatchSize + 1);
        for (E item : source) {
            Long id = idExtractor.apply(item);
            if (id == null) continue;
            batch.add(id);
            if (batch.size() >= inClauseBatchSize) {
                batchAction.accept(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            batchAction.accept(batch);
        }
    }

    private <R> List<R> executeAggregateQuery(Predicate predicate, AggregateSpec<T, R> spec) {

        validateAggregateSpec(spec);
        JPAQuery<Tuple> query = new JPAQuery<>(entityManager);
        query.from(path);

        AggregateJoinContext joinContext = new AggregateJoinContext(query, spec.getJoinStrategy());
        Map<String, PathResolution> resolvedPathCache = new HashMap<>(spec.getItems().size() + spec.getGroupByFields().size());
        List<AliasedExpression> selectExpressions = new ArrayList<>(spec.getItems().size());
        Map<String, AggregateProjection> projectionByTarget = new LinkedHashMap<>(spec.getItems().size());
        for (AggregateSpec.Item item : spec.getItems()) {
            PathResolution source = resolveAggregatePath(item.sourceField(), joinContext, resolvedPathCache);
            Expression<?> projected = buildAggregateExpression(item.type(), source.expression());
            Expression<?> aliased = ExpressionUtils.as(projected, item.targetField());
            selectExpressions.add(new AliasedExpression(item.targetField(), aliased));
            projectionByTarget.put(item.targetField(), new AggregateProjection(projected));
        }
        query.select(selectExpressions.stream().map(AliasedExpression::aliasedExpression).toArray(Expression[]::new));

        if (predicate != null) {
            query.where(predicate);
        }

        if (!spec.getGroupByFields().isEmpty()) {
            Expression<?>[] groupByExpr = spec.getGroupByFields().stream()
                    .map(field -> resolveAggregatePath(field, joinContext, resolvedPathCache).expression())
                    .toArray(Expression[]::new);
            query.groupBy(groupByExpr);
        }

        applyAggregateHaving(query, spec, projectionByTarget);
        applyAggregateOrder(query, spec, projectionByTarget);
        applyAggregatePage(query, spec);

        List<Tuple> rows = query.fetch();
        return rows.stream().map(tuple -> mapAggregateRow(tuple, spec, selectExpressions)).toList();
    }

    private void validateAggregateSpec(AggregateSpec<T, ?> spec) {

        Set<String> groupedFields = new HashSet<>(spec.getGroupByFields());
        Map<String, AggregateSpec.Item> itemByTarget = new LinkedHashMap<>(spec.getItems().size());
        Map<String, Class<?>> sourceTypeCache = new HashMap<>();

        for (AggregateSpec.Item item : spec.getItems()) {
            AggregateSpec.Item duplicate = itemByTarget.putIfAbsent(item.targetField(), item);
            if (duplicate != null) {
                throw new IllegalArgumentException("聚合目标字段重复映射: " + item.targetField());
            }

            Class<?> sourceType = sourceTypeCache.computeIfAbsent(
                    item.sourceField(),
                    field -> resolveFieldType(path.getType(), field)
            );
            if ((item.type() == AggType.SUM || item.type() == AggType.AVG) && !Number.class.isAssignableFrom(boxed(sourceType))) {
                throw new IllegalArgumentException("聚合字段必须是数值类型: " + item.sourceField() + " for " + item.type());
            }
            if (item.type() == AggType.FIELD && !groupedFields.contains(item.sourceField())) {
                throw new IllegalArgumentException("FIELD 投影字段必须出现在 groupBy 中: " + item.sourceField());
            }
        }

        for (AggregateSpec.Having having : spec.getHavings()) {
            if (!itemByTarget.containsKey(having.targetField())) {
                throw new IllegalArgumentException("having 字段未映射: " + having.targetField());
            }
            if (having.queryType() != com.dev.lib.entity.dsl.QueryType.IS_NULL
                    && having.queryType() != com.dev.lib.entity.dsl.QueryType.IS_NOT_NULL
                    && having.value() == null) {
                throw new IllegalArgumentException("having 条件值不能为空: " + having.targetField());
            }
        }

        for (AggregateSpec.Order order : spec.getOrders()) {
            if (!itemByTarget.containsKey(order.targetField())) {
                throw new IllegalArgumentException("order 字段未映射: " + order.targetField());
            }
        }
    }

    private PathResolution resolveAggregatePath(String fieldPath, AggregateJoinContext joinContext, Map<String, PathResolution> resolvedPathCache) {

        return resolvedPathCache.computeIfAbsent(fieldPath, field -> resolveAggregatePath(field, joinContext));
    }

    private PathResolution resolveAggregatePath(String fieldPath, AggregateJoinContext joinContext) {

        String[] parts = fieldPath.split("\\.");
        if (parts.length == 0) {
            throw new IllegalArgumentException("无效字段路径: " + fieldPath);
        }

        if (joinContext.joinStrategy() == com.dev.lib.entity.dsl.agg.AggJoinStrategy.AUTO || parts.length == 1) {
            return resolvePathImplicit(fieldPath, parts);
        }
        return resolvePathWithJoin(fieldPath, parts, joinContext);
    }

    private PathResolution resolvePathImplicit(String fieldPath, String[] parts) {

        PathBuilder<?> current = pathBuilder;
        Class<?> currentType = path.getType();

        for (int i = 0; i < parts.length - 1; i++) {
            Field field = findField(currentType, parts[i]);
            if (field == null) {
                throw new IllegalArgumentException("字段不存在: " + fieldPath);
            }
            currentType = resolveFieldType(field);
            current = current.get(parts[i], currentType);
        }

        String last = parts[parts.length - 1];
        Field field = findField(currentType, last);
        if (field == null) {
            throw new IllegalArgumentException("字段不存在: " + fieldPath);
        }
        Class<?> valueType = resolveFieldType(field);
        return new PathResolution(current.get(last));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private PathResolution resolvePathWithJoin(String fieldPath, String[] parts, AggregateJoinContext joinContext) {

        PathBuilder<?> currentPath = pathBuilder;
        Class<?> currentType = path.getType();
        StringBuilder relationPathBuilder = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Field field = findField(currentType, part);
            if (field == null) {
                throw new IllegalArgumentException("字段不存在: " + fieldPath);
            }
            Class<?> nextType = resolveFieldType(field);
            boolean relation = isJpaRelationField(field);

            if (relationPathBuilder.length() > 0) {
                relationPathBuilder.append('.');
            }
            relationPathBuilder.append(part);

            if (relation) {
                String joinKey = relationPathBuilder.toString();
                PathBuilder<?> alias = joinContext.joinedAliases().get(joinKey);
                if (alias == null) {
                    alias = new PathBuilder<>(nextType, "__agg_join_" + joinContext.joinedAliases().size());
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        CollectionPath collectionPath = currentPath.getCollection(part, nextType);
                        if (joinContext.joinStrategy() == com.dev.lib.entity.dsl.agg.AggJoinStrategy.LEFT) {
                            joinContext.query().leftJoin(collectionPath, alias);
                        } else {
                            joinContext.query().innerJoin(collectionPath, alias);
                        }
                    } else {
                        PathBuilder<?> relationPath = currentPath.get(part, nextType);
                        if (joinContext.joinStrategy() == com.dev.lib.entity.dsl.agg.AggJoinStrategy.LEFT) {
                            joinContext.query().leftJoin((EntityPath) relationPath, (Path) alias);
                        } else {
                            joinContext.query().innerJoin((EntityPath) relationPath, (Path) alias);
                        }
                    }
                    joinContext.joinedAliases().put(joinKey, alias);
                }
                currentPath = alias;
            } else {
                currentPath = currentPath.get(part, nextType);
            }

            currentType = nextType;
        }

        String last = parts[parts.length - 1];
        Field field = findField(currentType, last);
        if (field == null) {
            throw new IllegalArgumentException("字段不存在: " + fieldPath);
        }
        Class<?> valueType = resolveFieldType(field);
        return new PathResolution(currentPath.get(last));
    }

    private Expression<?> buildAggregateExpression(AggType type, Expression<?> source) {

        return switch (type) {
            case FIELD -> source;
            case COUNT -> Expressions.numberTemplate(Long.class, "count({0})", source);
            case COUNT_DISTINCT -> Expressions.numberTemplate(Long.class, "count(distinct {0})", source);
            case SUM -> Expressions.numberTemplate(BigDecimal.class, "sum({0})", source);
            case MIN -> Expressions.comparableTemplate(Comparable.class, "min({0})", source);
            case MAX -> Expressions.comparableTemplate(Comparable.class, "max({0})", source);
            case AVG -> Expressions.numberTemplate(BigDecimal.class, "avg({0})", source);
        };
    }

    private void applyAggregateHaving(
            JPAQuery<Tuple> query,
            AggregateSpec<T, ?> spec,
            Map<String, AggregateProjection> projectionByTarget
    ) {

        if (spec.getHavings().isEmpty()) {
            return;
        }

        BooleanBuilder havingBuilder = new BooleanBuilder();
        for (AggregateSpec.Having having : spec.getHavings()) {
            AggregateProjection projection = requireAggregateProjection(projectionByTarget, having.targetField(), "having");
            Predicate predicate = buildHavingPredicate(
                    projection.expression(),
                    having.queryType(),
                    having.value()
            );
            if (predicate != null) {
                havingBuilder.and(predicate);
            }
        }

        if (havingBuilder.getValue() != null) {
            query.having(havingBuilder.getValue());
        }
    }

    private Predicate buildHavingPredicate(
            Expression<?> expression,
            com.dev.lib.entity.dsl.QueryType queryType,
            Object value
    ) {

        if (queryType == null || queryType == com.dev.lib.entity.dsl.QueryType.EMPTY) {
            throw new IllegalArgumentException("having 查询类型不能为空");
        }

        return switch (queryType) {
            case EQ -> Expressions.booleanTemplate("{0} = {1}", expression, Expressions.constant(value));
            case NE -> Expressions.booleanTemplate("{0} <> {1}", expression, Expressions.constant(value));
            case GT -> Expressions.booleanTemplate("{0} > {1}", expression, Expressions.constant(value));
            case GE -> Expressions.booleanTemplate("{0} >= {1}", expression, Expressions.constant(value));
            case LT -> Expressions.booleanTemplate("{0} < {1}", expression, Expressions.constant(value));
            case LE -> Expressions.booleanTemplate("{0} <= {1}", expression, Expressions.constant(value));
            case LIKE -> Expressions.stringTemplate("str({0})", expression).containsIgnoreCase(String.valueOf(value));
            case START_WITH -> Expressions.stringTemplate("str({0})", expression).startsWithIgnoreCase(String.valueOf(value));
            case END_WITH -> Expressions.stringTemplate("str({0})", expression).endsWithIgnoreCase(String.valueOf(value));
            case IN -> {
                if (!(value instanceof Collection<?> values) || values.isEmpty()) {
                    yield null;
                }
                yield Expressions.booleanTemplate("{0} in {1}", expression, Expressions.constant(values));
            }
            case NOT_IN -> {
                if (!(value instanceof Collection<?> values) || values.isEmpty()) {
                    yield null;
                }
                yield Expressions.booleanTemplate("{0} not in {1}", expression, Expressions.constant(values));
            }
            case IS_NULL -> Expressions.booleanTemplate("{0} is null", expression);
            case IS_NOT_NULL -> Expressions.booleanTemplate("{0} is not null", expression);
            default -> throw new IllegalArgumentException("having 不支持查询类型: " + queryType);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applyAggregateOrder(
            JPAQuery<Tuple> query,
            AggregateSpec<T, ?> spec,
            Map<String, AggregateProjection> projectionByTarget
    ) {

        if (spec.getOrders().isEmpty()) {
            return;
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>(spec.getOrders().size());
        for (AggregateSpec.Order order : spec.getOrders()) {
            AggregateProjection projection = requireAggregateProjection(projectionByTarget, order.targetField(), "order");
            Expression<Comparable> orderExpression = Expressions.comparableTemplate(
                    Comparable.class,
                    "{0}",
                    projection.expression()
            );
            orders.add(new OrderSpecifier(order.asc() ? Order.ASC : Order.DESC, orderExpression));
        }
        query.orderBy(orders.toArray(OrderSpecifier[]::new));
    }

    private AggregateProjection requireAggregateProjection(
            Map<String, AggregateProjection> projectionByTarget,
            String targetField,
            String stage
    ) {

        AggregateProjection projection = projectionByTarget.get(targetField);
        if (projection == null) {
            throw new IllegalArgumentException(stage + " 字段未映射: " + targetField);
        }
        return projection;
    }

    private void applyAggregatePage(JPAQuery<Tuple> query, AggregateSpec<T, ?> spec) {

        if (spec.getOffset() != null) {
            query.offset(spec.getOffset());
        }
        if (spec.getLimit() != null) {
            query.limit(spec.getLimit());
        }
    }

    private <R> R mapAggregateRow(
            Tuple tuple,
            AggregateSpec<T, R> spec,
            List<AliasedExpression> selectExpressions
    ) {

        Map<String, Object> valueByField = new HashMap<>(selectExpressions.size());
        for (int i = 0; i < selectExpressions.size(); i++) {
            AliasedExpression aliasedExpression = selectExpressions.get(i);
            Object value = tuple.get(i, Object.class);
            valueByField.put(aliasedExpression.targetField(), value);
        }

        AggregateCtorPlan ctorPlan = getAggregateCtorPlan(spec.getTargetClass());
        return switch (ctorPlan.type()) {
            case NO_ARGS -> mapByNoArgsCtor(spec.getTargetClass(), valueByField, ctorPlan.constructor());
            case ALL_ARGS -> mapByArgsCtor(spec.getTargetClass(), valueByField, ctorPlan);
        };
    }

    @SuppressWarnings("unchecked")
    private <R> R mapByNoArgsCtor(Class<R> targetClass, Map<String, Object> valueByField, Constructor<?> constructor) {

        R instance;
        try {
            instance = (R) constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("聚合结果对象构造失败: " + targetClass.getName(), e);
        }

        Map<String, Method> setters = getAggTargetSetters(targetClass);
        Map<String, Field> fields = getAggTargetFields(targetClass);
        for (Map.Entry<String, Object> entry : valueByField.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            Method setter = setters.get(fieldName);
            if (setter != null) {
                ReflectionUtils.makeAccessible(setter);
                ReflectionUtils.invokeMethod(
                        setter,
                        instance,
                        convertAggregateValue(value, setter.getParameterTypes()[0])
                );
                continue;
            }

            Field field = fields.get(fieldName);
            if (field != null) {
                ReflectionUtils.setField(field, instance, convertAggregateValue(value, field.getType()));
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private <R> R mapByArgsCtor(Class<R> targetClass, Map<String, Object> valueByField, AggregateCtorPlan plan) {

        try {
            Constructor<?> constructor = plan.constructor();
            String[] names = plan.argNames();
            Class<?>[] argTypes = constructor.getParameterTypes();
            Object[] args = new Object[argTypes.length];
            for (int i = 0; i < argTypes.length; i++) {
                args[i] = convertAggregateValue(valueByField.get(names[i]), argTypes[i]);
            }
            return (R) constructor.newInstance(args);
        } catch (Exception e) {
            throw new IllegalStateException("聚合结果对象构造失败: " + targetClass.getName(), e);
        }
    }

    private AggregateCtorPlan getAggregateCtorPlan(Class<?> targetClass) {

        return AGG_TARGET_CTOR_PLAN_CACHE.computeIfAbsent(targetClass, this::buildAggregateCtorPlan);
    }

    private AggregateCtorPlan buildAggregateCtorPlan(Class<?> targetClass) {

        Constructor<?> noArgsCtor = AGG_TARGET_CTOR_CACHE.computeIfAbsent(targetClass, clazz -> {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                ReflectionUtils.makeAccessible(constructor);
                return constructor;
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
        if (noArgsCtor != null) {
            return new AggregateCtorPlan(AggregateCtorPlanType.NO_ARGS, noArgsCtor, new String[0]);
        }

        if (targetClass.isRecord()) {
            return buildRecordCtorPlan(targetClass);
        }

        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            ConstructorProperties properties = constructor.getAnnotation(ConstructorProperties.class);
            if (properties != null) {
                String[] names = properties.value();
                if (names.length == constructor.getParameterCount()) {
                    ReflectionUtils.makeAccessible(constructor);
                    return new AggregateCtorPlan(AggregateCtorPlanType.ALL_ARGS, constructor, names);
                }
            }
        }

        if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
            Constructor<?> constructor = constructors[0];
            String[] names = Arrays.stream(constructor.getParameters()).map(Parameter::getName).toArray(String[]::new);
            boolean hasRealNames = Arrays.stream(names).noneMatch(name -> name.startsWith("arg"));
            if (hasRealNames) {
                ReflectionUtils.makeAccessible(constructor);
                return new AggregateCtorPlan(AggregateCtorPlanType.ALL_ARGS, constructor, names);
            }
        }

        throw new IllegalStateException("聚合结果类型必须有无参构造、Record 构造或 @ConstructorProperties 构造: " + targetClass.getName());
    }

    private AggregateCtorPlan buildRecordCtorPlan(Class<?> targetClass) {

        try {
            RecordComponent[] components = targetClass.getRecordComponents();
            Class<?>[] argTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);
            String[] argNames = Arrays.stream(components).map(RecordComponent::getName).toArray(String[]::new);
            Constructor<?> constructor = targetClass.getDeclaredConstructor(argTypes);
            ReflectionUtils.makeAccessible(constructor);
            return new AggregateCtorPlan(AggregateCtorPlanType.ALL_ARGS, constructor, argNames);
        } catch (Exception e) {
            throw new IllegalStateException("Record 聚合结果构造解析失败: " + targetClass.getName(), e);
        }
    }

    private Map<String, Method> getAggTargetSetters(Class<?> targetClass) {

        return AGG_TARGET_SETTER_CACHE.computeIfAbsent(targetClass, clazz -> {
            Map<String, Method> setters = new HashMap<>();
            for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getParameterCount() != 1 || !method.getName().startsWith("set") || method.getName().length() <= 3) {
                        continue;
                    }
                    String name = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                    ReflectionUtils.makeAccessible(method);
                    setters.putIfAbsent(name, method);
                }
            }
            return setters;
        });
    }

    private Object convertAggregateValue(Object value, Class<?> rawTargetType) {

        if (value == null) {
            return null;
        }
        Class<?> targetType = boxed(rawTargetType);
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == Long.class && value instanceof Number number) {
            return number.longValue();
        }
        if (targetType == Integer.class && value instanceof Number number) {
            return number.intValue();
        }
        if (targetType == Double.class && value instanceof Number number) {
            return number.doubleValue();
        }
        if (targetType == Float.class && value instanceof Number number) {
            return number.floatValue();
        }
        if (targetType == Short.class && value instanceof Number number) {
            return number.shortValue();
        }
        if (targetType == Byte.class && value instanceof Number number) {
            return number.byteValue();
        }
        if (targetType == BigDecimal.class && value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return value;
    }

    private Map<String, Field> getAggTargetFields(Class<?> targetClass) {

        return AGG_TARGET_FIELD_CACHE.computeIfAbsent(targetClass, clazz -> {
            Map<String, Field> fields = new HashMap<>();
            for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    ReflectionUtils.makeAccessible(field);
                    fields.putIfAbsent(field.getName(), field);
                }
            }
            return fields;
        });
    }

    private Field findField(Class<?> type, String name) {

        Class<?> current = type;
        while (current != null && current != Object.class) {
            Field field = ReflectionUtils.findField(current, name);
            if (field != null) {
                ReflectionUtils.makeAccessible(field);
                return field;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Class<?> resolveFieldType(Class<?> rootType, String fieldPath) {

        String[] parts = fieldPath.split("\\.");
        Class<?> currentType = rootType;
        for (String part : parts) {
            Field field = findField(currentType, part);
            if (field == null) {
                throw new IllegalArgumentException("字段不存在: " + fieldPath);
            }
            currentType = resolveFieldType(field);
        }
        return currentType;
    }

    private Class<?> resolveFieldType(Field field) {

        if (!Collection.class.isAssignableFrom(field.getType())) {
            return boxed(field.getType());
        }

        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> argType) {
                return boxed(argType);
            }
        }
        return Object.class;
    }

    private boolean isJpaRelationField(Field field) {

        return field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToOne.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    private Class<?> boxed(Class<?> type) {

        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private record AliasedExpression(String targetField, Expression<?> aliasedExpression) {
    }

    private record AggregateProjection(Expression<?> expression) {
    }

    private record PathResolution(Expression<?> expression) {
    }

    private record AggregateJoinContext(
            JPAQuery<Tuple> query,
            com.dev.lib.entity.dsl.agg.AggJoinStrategy joinStrategy,
            Map<String, PathBuilder<?>> joinedAliases
    ) {

        AggregateJoinContext(JPAQuery<Tuple> query, com.dev.lib.entity.dsl.agg.AggJoinStrategy joinStrategy) {

            this(query, joinStrategy, new LinkedHashMap<>());
        }
    }

    private enum AggregateCtorPlanType {
        NO_ARGS,
        ALL_ARGS
    }

    private record AggregateCtorPlan(AggregateCtorPlanType type, Constructor<?> constructor, String[] argNames) {
    }

    private void ensureNonAggregateQuery(DslQuery<T> dslQuery, String operation) {

        if (dslQuery != null && dslQuery.hasAgg()) {
            throw new IllegalStateException("检测到 agg() 聚合配置，" + operation + " 不支持聚合查询，请使用 aggregate()");
        }
    }

    // ==================== Predicate 构建 ====================

    Predicate buildPredicate(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        BooleanBuilder builder = new BooleanBuilder();

        switch (ctx.getDeletedFilter()) {
            case EXCLUDE_DELETED -> builder.and(deletedPath.eq(false));
            case ONLY_DELETED -> builder.and(deletedPath.eq(true));
        }

        BooleanExpression pluginExpr = QueryPluginChain.getInstance().apply(pathBuilder, path.getType());
        if (pluginExpr != null) {
            builder.and(pluginExpr);
        }

        Predicate dsl = toPredicate(dslQuery, expressions);
        if (dsl != null) builder.and(dsl);
        return builder.getValue();
    }

    private static <E extends JpaEntity> Predicate toPredicate(DslQuery<E> query, BooleanExpression... expressions) {

        if (query != null) {
            Collection<QueryFieldMerger.FieldMetaValue> merged = DslQueryFieldResolver.resolveMerged(
                    query,
                    DslQueryFieldResolver.OverridePolicy.SELF_OVERRIDE_EXTERNAL
            );
            return PredicateAssembler.assemble(query, merged, expressions);
        }
        return expressions.length == 0 ? null : PredicateAssembler.assemble(null, null, expressions);
    }

    private boolean isEmptyPredicate(Predicate predicate) {

        if (predicate == null) return true;
        if (predicate instanceof BooleanBuilder bb) {
            return !bb.hasValue();
        }
        return false;
    }

    // ==================== 工具方法 ====================

    @SuppressWarnings("unchecked")
    private <P> P resolvePath(String fieldName, Class<P> type) {

        try {
            return (P) path.getClass().getField(fieldName).get(path);
        } catch (Exception e) {
            throw new IllegalStateException("实体缺少 " + fieldName + " 字段", e);
        }
    }

    private void applySort(JPAQuery<?> query, DslQuery<T> dslQuery) {

        Sort sort = dslQuery.toSort(getAllowFields(dslQuery));
        applySort(query, sort);
    }

    private void applySort(JPAQuery<?> query, Sort sort) {

        if (sort.isUnsorted()) return;

        for (Sort.Order order : sort) {
            query.orderBy(new OrderSpecifier<>(
                    order.isAscending() ? Order.ASC : Order.DESC,
                    pathBuilder.getComparable(order.getProperty(), Comparable.class)
            ));
        }
    }

    private void applyLimit(JPAQuery<?> query, DslQuery<T> dslQuery) {

        if (dslQuery.getLimit() != null) query.limit(dslQuery.getLimit());
        if (dslQuery.getOffset() != null) query.offset(dslQuery.getOffset());
    }

    private void applyLockOptions(JPAQuery<?> query, QueryContext ctx) {

        if (!ctx.hasLock()) {
            return;
        }
        query.setLockMode(ctx.getLockMode());
        if (ctx.isSkipLocked()) {
            query.setHint("org.hibernate.lockMode", "UPGRADE_SKIPLOCKED");
        }
    }

    private void applyStreamFetchSize(JPAQuery<?> query) {

        query.setHint(HibernateHints.HINT_FETCH_SIZE, jdbcBatchSize * 2);
    }

    private Set<String> getAllowFields(DslQuery<T> dslQuery) {

        if (dslQuery == null) return Collections.emptySet();
        return FieldMetaCache.getMeta(dslQuery.getClass()).entityFieldNames();
    }

    private PathBuilder<?> createPathBuilder(Class<?> clazz) {

        return PATH_BUILDER_CACHE.computeIfAbsent(clazz, this::newPathBuilder);
    }

    private PathBuilder<?> newPathBuilder(Class<?> clazz) {

        String entityName   = clazz.getSimpleName();
        String variableName = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
        return new PathBuilder<>(clazz, variableName);
    }

    // ==================== 级联字段处理 ====================

    private List<T> ensureCascadeFieldsLoaded(List<T> entities) {

        if (entities.isEmpty()) return entities;

        boolean needsReload = entities.stream().anyMatch(this::hasUninitializedCascadeFields);
        if (!needsReload) return entities;

        List<Long> ids = entities.stream().map(JpaEntity::getId).toList();

        JPAQuery<T> query = queryFactory.selectFrom(path)
                .where(pathBuilder.getNumber("id", Long.class).in(ids));

        List<Field> cascadeFields = getCascadeFields(path.getType());
        for (Field field : cascadeFields) {
            query.leftJoin(pathBuilder.getCollection(field.getName(), field.getType())).fetchJoin();
        }

        return query.fetch();
    }

    private boolean hasUninitializedCascadeFields(T entity) {

        List<Field> cascadeFields = getCascadeFields(Hibernate.getClass(entity));
        for (Field field : cascadeFields) {
            Object value = ReflectionUtils.getField(field, entity);
            if (!Hibernate.isInitialized(value)) {
                return true;
            }
        }
        return false;
    }

    private void collectCascadeEntities(Object entity, Set<Object> visited, Map<Class<?>, Set<Long>> toDeleteByType) {

        if (entity == null || visited.contains(entity)) return;
        visited.add(entity);

        Class<?> realClass = Hibernate.getClass(entity);

        if (entity instanceof JpaEntity jpaEntity) {
            if (Boolean.TRUE.equals(jpaEntity.getDeleted())) return;

            toDeleteByType
                    .computeIfAbsent(realClass, k -> new LinkedHashSet<>())
                    .add(jpaEntity.getId());
        }

        List<Field> cascadeFields = getCascadeFields(realClass);
        for (Field field : cascadeFields) {
            Object value = ReflectionUtils.getField(field, entity);

            if (!Hibernate.isInitialized(value)) {
                Hibernate.initialize(value);
            }

            if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    collectCascadeEntities(item, visited, toDeleteByType);
                }
            } else if (value != null) {
                collectCascadeEntities(value, visited, toDeleteByType);
            }
        }
    }

    private List<Field> getCascadeFields(Class<?> clazz) {

        return CASCADE_FIELDS_CACHE.computeIfAbsent(clazz, this::resolveCascadeFields);
    }

    private List<Field> resolveCascadeFields(Class<?> clazz) {

        List<Field> result = new ArrayList<>();

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (shouldCascadeRemove(field)) {
                    ReflectionUtils.makeAccessible(field);
                    result.add(field);
                }
            }
            current = current.getSuperclass();
        }

        return result;
    }

    private boolean shouldCascadeRemove(Field field) {

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null && (hasCascadeRemove(oneToMany.cascade()) || oneToMany.orphanRemoval())) {
            return true;
        }

        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        return oneToOne != null && (hasCascadeRemove(oneToOne.cascade()) || oneToOne.orphanRemoval());
    }

    private boolean hasCascadeRemove(CascadeType[] cascadeTypes) {

        for (CascadeType type : cascadeTypes) {
            if (type == CascadeType.REMOVE || type == CascadeType.ALL) {
                return true;
            }
        }
        return false;
    }

    // ==================== 委托 QueryDSL ====================

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
            @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {

        return querydslExecutor.findBy(predicate, queryFunction);
    }

}
