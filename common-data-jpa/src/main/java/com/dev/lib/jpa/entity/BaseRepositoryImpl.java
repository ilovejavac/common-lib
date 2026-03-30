package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.TransactionHelper;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.jpa.entity.dsl.SelectBuilder;
import com.dev.lib.jpa.entity.dsl.plugin.QueryPluginChain;
import com.dev.lib.jpa.entity.insert.EntityMeta;
import com.dev.lib.jpa.entity.insert.EntityMetaCache;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.util.StringUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateHints;
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

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseRepositoryImpl<T extends JpaEntity> extends SimpleJpaRepository<T, Long> implements BaseRepository<T> {

    private static final int DEFAULT_BATCH_SIZE = 1024;

    private static final Expression<Long> WINDOW_TOTAL_EXPRESSION = Expressions.numberTemplate(Long.class, "count(*) over()");

    private static final Map<Class<?>, List<Field>> CASCADE_FIELDS_CACHE = new ConcurrentHashMap<>(128);

    private final EntityManager entityManager;

    private final JPAQueryFactory queryFactory;

    private final EntityPath<T> path;

    private final PathBuilder<T> pathBuilder;

    private final QuerydslJpaPredicateExecutor<T> querydslExecutor;

    private final BooleanPath deletedPath;

    private final NumberPath<Long> idPath;

    private final Class<T> entityClass;

    private final boolean hasCascadeFields;

    private final int batchSize;

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
        this.batchSize = resolveConfiguredBatchSize(em);
    }

    private int resolveConfiguredBatchSize(EntityManager em) {

        Object configured = em.getEntityManagerFactory().getProperties().get("hibernate.jdbc.batch_size");
        return normalizeBatchSize(configured, DEFAULT_BATCH_SIZE);
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

    // ==================== 核心查询方法（统一入口）====================

    /**
     * 统一查询单条
     *
     * @param ctx         查询上下文
     * @param select      字段选择器，null 表示全字段
     * @param resultClass 返回类型
     */
    @SuppressWarnings("unchecked")
    <D> Optional<D> load(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        // 全字段查询走优化路径
        if (select == null && resultClass == entityClass) {
            return (Optional<D>) loadFullEntity(ctx, dslQuery, expressions);
        }

        List<D> results = doQuery(ctx, select, resultClass, dslQuery, expressions, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * 统一查询多条
     */
    @SuppressWarnings("unchecked")
    <D> List<D> loads(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        if (select == null && resultClass == entityClass) {
            return (List<D>) loadsFullEntity(ctx, dslQuery, expressions);
        }

        return doQuery(ctx, select, resultClass, dslQuery, expressions, null);
    }

    /**
     * 统一分页查询
     */
    @SuppressWarnings("unchecked")
    <D> Page<D> page(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("分页不支持加锁");
        }

        if (select == null && resultClass == entityClass) {
            return (Page<D>) pageFullEntityWithWindowCount(ctx, dslQuery, expressions);
        }

        List<D> content;
        long total;
        try {
            WindowPageResult<D> pageResult = doQueryWithWindowCount(ctx, select, resultClass, dslQuery, expressions);
            content = pageResult.content();
            total = pageResult.total();
        } catch (RuntimeException ex) {
            if (!shouldFallbackToLegacyPage(ex)) {
                throw ex;
            }
            content = doQuery(ctx, select, resultClass, dslQuery, expressions, null);
            total = count(ctx, dslQuery, expressions);
        }
        return new PageImpl<>(content, dslQuery.toPageable(null), total);
    }

    /**
     * 统一流式查询
     */
    @SuppressWarnings("unchecked")
    <D> Stream<D> stream(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("流式查询不支持加锁");
        }

        if (select == null && resultClass == entityClass) {
            return (Stream<D>) streamFullEntity(ctx, dslQuery, expressions);
        }

        return doStreamQuery(ctx, select, resultClass, dslQuery, expressions);
    }

    /**
     * 统一计数
     */
    long count(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return querydslExecutor.count(buildPredicate(ctx, dslQuery, expressions));
    }

    /**
     * 统一存在判断
     */
    boolean exists(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return querydslExecutor.exists(buildPredicate(ctx, dslQuery, expressions));
    }

    BaseEntityListener listener = new BaseEntityListener();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <S extends T> List<S> saveAll(Iterable<S> entities) {

        if (entities == null) {
            return Collections.emptyList();
        }

        boolean appendToResult = true;
        List<S> result;
        if (entities instanceof List<S> source) {
            result = source;
            appendToResult = false;
        } else {
            result = new ArrayList<>();
        }
        List<S> writeBatch = new ArrayList<>(batchSize);

        for (S entity : entities) {
            if (entity == null) {
                continue;
            }
            listener.prePersist(entity);
            writeBatch.add(entity);
            if (appendToResult) {
                result.add(entity);
            }

            if (writeBatch.size() >= batchSize) {
                persistSaveBatch(writeBatch);
                writeBatch.clear();
            }
        }

        if (!writeBatch.isEmpty()) {
            persistSaveBatch(writeBatch);
        }

        return result;
    }

    private <S extends T> void persistSaveBatch(List<S> batch) {

        if (batch.isEmpty()) {
            return;
        }

        if (hasCascadeFields) {
            super.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
            return;
        }

        batchInsertNative(batch, batchSize);
    }

    private <S extends T> void batchInsertNative(List<S> entities, int effectiveBatchSize) {

        EntityMeta meta = EntityMetaCache.get(entityClass);

        String sql = meta.getInsertSql();

        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int count = 0;
                for (S entity : entities) {
                    if (entity == null) {
                        continue;
                    }
                    meta.setParameters(ps, entity);
                    ps.addBatch();

                    if (++count % effectiveBatchSize == 0) {
                        ps.executeBatch();
                    }
                }
                if (count % effectiveBatchSize != 0) {
                    ps.executeBatch();
                }
            }
        });
    }

    // ==================== 内部查询实现 ====================

    /**
     * 执行部分字段查询
     */
    @SuppressWarnings("unchecked")
    private <D> List<D> doQuery(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression[] expressions, Integer limit) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        Expression<?>[] selectExprs = select.buildExpressions(pathBuilder);

        JPAQuery<Tuple> query = queryFactory.select(selectExprs).from(path);

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);
        if (predicate != null) {
            query.where(predicate);
        }

        if (ctx.hasLock()) {
            query.setLockMode(ctx.getLockMode());
        }

        if (dslQuery != null) {
            applySort(query, dslQuery);
            if (limit == null) {
                applyLimit(query, dslQuery);
            }
        }

        if (limit != null) {
            query.limit(limit);
        }

        List<Tuple> tuples = query.fetch();

        if (resultClass == entityClass) {
            return (List<D>) tuples.stream().map(select::toEntity).toList();
        }
        return tuples.stream().map(t -> select.toDto(t, resultClass)).toList();
    }

    @SuppressWarnings("unchecked")
    private <D> WindowPageResult<D> doQueryWithWindowCount(
            QueryContext ctx,
            SelectBuilder<T> select,
            Class<D> resultClass,
            DslQuery<T> dslQuery,
            BooleanExpression[] expressions) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        Expression<?>[] selectExprs = select.buildExpressions(pathBuilder);
        Expression<?>[] queryExprs = Arrays.copyOf(selectExprs, selectExprs.length + 1);
        queryExprs[selectExprs.length] = WINDOW_TOTAL_EXPRESSION;

        JPAQuery<Tuple> query = queryFactory.select(queryExprs).from(path);

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);
        if (predicate != null) {
            query.where(predicate);
        }

        if (ctx.hasLock()) {
            query.setLockMode(ctx.getLockMode());
        }

        if (dslQuery != null) {
            applySort(query, dslQuery);
            applyLimit(query, dslQuery);
        }

        List<Tuple> tuples = query.fetch();

        if (tuples.isEmpty()) {
            return new WindowPageResult<>(Collections.emptyList(), count(ctx, dslQuery, expressions));
        }

        List<D> content;
        if (resultClass == entityClass) {
            content = (List<D>) tuples.stream().map(select::toEntity).toList();
        } else {
            content = tuples.stream().map(t -> select.toDto(t, resultClass)).toList();
        }

        long total = resolveWindowTotal(tuples, WINDOW_TOTAL_EXPRESSION, () -> count(ctx, dslQuery, expressions));
        return new WindowPageResult<>(content, total);
    }

    static long resolveWindowTotal(List<Tuple> tuples, Expression<Long> totalExpression, java.util.function.LongSupplier countFallback) {

        if (tuples == null || tuples.isEmpty()) {
            return countFallback.getAsLong();
        }

        Long total = tuples.getFirst().get(totalExpression);
        if (total != null) {
            return total;
        }
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
    @SuppressWarnings("unchecked")
    private <D> Stream<D> doStreamQuery(QueryContext ctx, SelectBuilder<T> select, Class<D> resultClass, DslQuery<T> dslQuery, BooleanExpression[] expressions) {

        Objects.requireNonNull(select, "部分字段查询必须指定 SelectBuilder");

        Expression<?>[] selectExprs = select.buildExpressions(pathBuilder);

        JPAQuery<Tuple> query = queryFactory.select(selectExprs).from(path);

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);
        if (predicate != null) {
            query.where(predicate);
        }

        query.setHint(HibernateHints.HINT_FETCH_SIZE, batchSize * 2);

        if (dslQuery != null) {
            applySort(query, dslQuery);
        }

        if (resultClass == entityClass) {
            return (Stream<D>) query.stream().map(select::toEntity);
        }
        return query.stream().map(t -> select.toDto(t, resultClass));
    }

    private record WindowPageResult<D>(List<D> content, long total) {
    }

    // ==================== 全字段查询（优化路径）====================

    private Optional<T> loadFullEntity(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);

        if (ctx.hasLock()) {
            List<T> result = queryFactory.selectFrom(path)
                    .where(predicate)
                    .setLockMode(ctx.getLockMode())
                    .limit(1)
                    .fetch();
            return result.stream().findFirst();
        }

        return querydslExecutor.findOne(predicate);
    }

    private List<T> loadsFullEntity(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);

        if (ctx.hasLock()) {
            JPAQuery<T> query = queryFactory.selectFrom(path)
                    .where(predicate)
                    .setLockMode(ctx.getLockMode());
            if (dslQuery != null) {
                applySort(query, dslQuery);
                applyLimit(query, dslQuery);
            }
            return query.fetch();
        }

        if (dslQuery != null && dslQuery.getLimit() != null) {
            return querydslExecutor.findAll(predicate, dslQuery.toPageable(getAllowFields(dslQuery)))
                    .getContent();
        }

        Sort sort = dslQuery != null ? dslQuery.toSort(getAllowFields(dslQuery)) : Sort.unsorted();
        return querydslExecutor.findAll(predicate, sort);
    }

    private Page<T> pageFullEntity(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);
        return querydslExecutor.findAll(predicate, dslQuery.toPageable(getAllowFields(dslQuery)));
    }

    private Page<T> pageFullEntityWithWindowCount(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        try {
            return doPageFullEntityWithWindowCount(ctx, dslQuery, expressions);
        } catch (RuntimeException ex) {
            if (!shouldFallbackToLegacyPage(ex)) {
                throw ex;
            }
            return pageFullEntity(ctx, dslQuery, expressions);
        }
    }

    private Page<T> doPageFullEntityWithWindowCount(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);
        Pageable pageable = dslQuery.toPageable(getAllowFields(dslQuery));

        JPAQuery<Tuple> query = queryFactory.select(path, WINDOW_TOTAL_EXPRESSION).from(path);
        if (predicate != null) {
            query.where(predicate);
        }

        applySort(query, pageable.getSort());
        query.offset(pageable.getOffset());
        query.limit(pageable.getPageSize());

        List<Tuple> tuples = query.fetch();
        List<T> content = tuples.stream()
                .map(tuple -> tuple.get(path))
                .filter(Objects::nonNull)
                .toList();
        long total = resolveWindowTotal(tuples, WINDOW_TOTAL_EXPRESSION, () -> count(ctx, dslQuery, expressions));
        return new PageImpl<>(content, pageable, total);
    }

    private Stream<T> streamFullEntity(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);

        JPAQuery<T> query = queryFactory.selectFrom(path).where(predicate);
        query.setHint(HibernateHints.HINT_FETCH_SIZE, batchSize * 2);

        if (dslQuery != null) {
            applySort(query, dslQuery);
        }

        return query.stream();
    }

    // ==================== 删除 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        delete(new QueryContext(), dslQuery, expressions);
    }

    void delete(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate businessPredicate = toPredicate(dslQuery, expressions);
        if (isEmptyPredicate(businessPredicate)) {
            throw new IllegalArgumentException("批量删除必须指定业务条件，防止误删全表");
        }

        if (hasCascadeFields) {
            Predicate fullPredicate = buildPredicate(ctx, dslQuery, expressions);
            softDeleteByPredicateInChunks(fullPredicate);
        } else {
            BooleanBuilder where = new BooleanBuilder();
            if (ctx.getDeletedFilter() == QueryContext.DeletedFilter.EXCLUDE_DELETED) {
                where.and(deletedPath.eq(false));
            }
            where.and(businessPredicate);
            batchSoftDelete(where);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(T entity) {

        if (entity == null || entity.getId() == null) return;

        if (hasCascadeFields) {
            T managed = entityManager.contains(entity)
                        ? entity
                        : entityManager.find(path.getType(), entity.getId());
            if (managed != null) {
                cascadeSoftDeleteBatch(List.of(managed));
                return;
            }
        }

        BooleanExpression idCondition = pathBuilder.getNumber("id", Long.class).eq(entity.getId());
        batchSoftDelete(idCondition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {

        if (id == null) return;

        if (hasCascadeFields) {
            T managed = entityManager.find(path.getType(), id);
            if (managed != null) {
                cascadeSoftDeleteBatch(List.of(managed));
                return;
            }
        }

        BooleanExpression idCondition = pathBuilder.getNumber("id", Long.class).eq(id);
        batchSoftDelete(idCondition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(Iterable<? extends T> entities) {

        if (entities == null) return;

        List<Long> ids = new ArrayList<>(batchSize);
        for (T entity : entities) {
            if (entity == null || entity.getId() == null) {
                continue;
            }
            ids.add(entity.getId());
            if (ids.size() >= batchSize) {
                softDeleteByIds(ids);
                ids.clear();
            }
        }

        if (!ids.isEmpty()) {
            softDeleteByIds(ids);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllById(Iterable<? extends Long> ids) {

        if (ids == null) return;

        List<Long> batchIds = new ArrayList<>(batchSize);
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            batchIds.add(id);
            if (batchIds.size() >= batchSize) {
                softDeleteByIds(batchIds);
                batchIds.clear();
            }
        }

        if (!batchIds.isEmpty()) {
            softDeleteByIds(batchIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll() {

        Long lastId = null;
        while (true) {
            List<Long> ids = fetchActiveIdsAfter(lastId);
            if (ids.isEmpty()) {
                break;
            }
            softDeleteByIds(ids);
            lastId = ids.getLast();
        }
    }

    private void softDeleteByIds(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return;
        }

        if (!hasCascadeFields) {
            batchSoftDelete(idPath.in(ids));
            return;
        }

        List<T> toDelete = queryFactory.selectFrom(path)
                .where(idPath.in(ids))
                .where(deletedPath.eq(false))
                .fetch();
        if (!toDelete.isEmpty()) {
            cascadeSoftDeleteBatch(toDelete);
        }
    }

    private List<Long> fetchActiveIdsAfter(Long lastId) {

        JPAQuery<Long> idQuery = queryFactory.select(idPath)
                .from(path)
                .where(deletedPath.eq(false));
        if (lastId != null) {
            idQuery.where(idPath.gt(lastId));
        }
        return idQuery.orderBy(idPath.asc())
                .limit(batchSize)
                .fetch();
    }

    private void softDeleteByPredicateInChunks(Predicate fullPredicate) {

        Long lastId = null;
        while (true) {
            JPAQuery<Long> idQuery = queryFactory.select(idPath)
                    .from(path)
                    .where(fullPredicate);
            if (lastId != null) {
                idQuery.where(idPath.gt(lastId));
            }

            List<Long> ids = idQuery.orderBy(idPath.asc())
                    .limit(batchSize)
                    .fetch();
            if (ids.isEmpty()) {
                break;
            }

            softDeleteByIds(ids);
            lastId = ids.getLast();
        }
    }

    private void batchSoftDelete(Predicate businessPredicate) {

        BooleanBuilder where = new BooleanBuilder();
        where.and(deletedPath.eq(false));
        if (businessPredicate != null) {
            where.and(businessPredicate);
        }

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

            PathBuilder<?> builder = createPathBuilder(clazz);

            for (int i = 0; i < ids.size(); i += batchSize) {
                List<Long> batch = ids.subList(i, Math.min(i + batchSize, ids.size()));

                queryFactory.update(builder)
                        .set(builder.getBoolean("deleted"), true)
                        .set(builder.getDateTime("updatedAt", LocalDateTime.class), now)
                        .set(builder.getNumber("modifierId", Long.class), modifierId)
                        .where(builder.getNumber("id", Long.class).in(batch))
                        .execute();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    // ==================== 物理删除 ====================

    void hardDelete(T entity) {

        if (entity == null || entity.getId() == null) return;

        if (entityManager.contains(entity)) {
            entityManager.remove(entity);
            return;
        }

        T existing = entityManager.find(path.getType(), entity.getId());
        if (existing != null) {
            entityManager.remove(existing);
        }
    }

    void hardDeleteById(Long id) {

        if (id == null) return;

        T existing = entityManager.find(path.getType(), id);
        if (existing != null) {
            entityManager.remove(existing);
        }
    }

    void hardDeleteAll(Iterable<? extends T> entities) {

        if (entities == null) return;

        List<Long> ids = new ArrayList<>(batchSize);
        for (T entity : entities) {
            if (entity != null && entity.getId() != null) {
                ids.add(entity.getId());
            }
            if (ids.size() >= batchSize) {
                executeHardDeleteBatch(ids);
                ids.clear();
            }
        }

        if (!ids.isEmpty()) {
            executeHardDeleteBatch(ids);
        }
    }

    void hardDeleteAllById(Iterable<Long> ids) {

        if (ids == null) return;

        List<Long> list = new ArrayList<>(batchSize);
        for (Long id : ids) {
            if (id != null) {
                list.add(id);
            }
            if (list.size() >= batchSize) {
                executeHardDeleteBatch(list);
                list.clear();
            }
        }

        if (!list.isEmpty()) {
            executeHardDeleteBatch(list);
        }
    }

    private void executeHardDeleteBatch(List<Long> ids) {

        List<Long> batch = List.copyOf(ids);
        TransactionHelper.run(() -> batchHardDeleteById(batch));
    }

    void batchHardDeleteById(List<Long> list) {

        if (list == null || list.isEmpty()) {
            return;
        }

        for (int i = 0; i < list.size(); i += batchSize) {
            List<Long> batch = list.subList(i, Math.min(i + batchSize, list.size()));
            queryFactory.delete(path)
                    .where(idPath.in(batch))
                    .execute();
        }

        entityManager.flush();
        entityManager.clear();
    }

    long hardDelete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(new QueryContext().withDeleted(), dslQuery, expressions);

        long affected = queryFactory.delete(path)
                .where(predicate)
                .execute();

        entityManager.flush();
        entityManager.clear();

        return affected;
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
            List<QueryFieldMerger.FieldMetaValue>        self   = QueryFieldMerger.resolve(query);
            Map<String, QueryFieldMerger.FieldMetaValue> fields = new HashMap<>();
            query.getExternalFields().forEach(it -> fields.put(
                    StringUtils.format("{}-{}", it.getFieldMeta().targetField(), it.getFieldMeta().queryType()),
                    it
            ));
            for (QueryFieldMerger.FieldMetaValue fmv : self) {
                fields.put(
                        StringUtils.format("{}-{}", fmv.getFieldMeta().targetField(), fmv.getFieldMeta().queryType()),
                        fmv
                );
            }
            return PredicateAssembler.assemble(query, fields.values(), expressions);
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

    private Set<String> getAllowFields(DslQuery<T> dslQuery) {

        if (dslQuery == null) return Collections.emptySet();
        return Arrays.stream(FieldMetaCache.getMeta(dslQuery.getClass()).entityClass().getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    private PathBuilder<?> createPathBuilder(Class<?> clazz) {

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
    public Optional<T> findOne(Predicate predicate) {

        return querydslExecutor.findOne(predicate);
    }

    @Override
    public List<T> findAll(Predicate predicate) {

        return querydslExecutor.findAll(predicate);
    }

    @Override
    public List<T> findAll(Predicate predicate, Sort sort) {

        return querydslExecutor.findAll(predicate, sort);
    }

    @Override
    public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

        return querydslExecutor.findAll(predicate, orders);
    }

    @Override
    public List<T> findAll(OrderSpecifier<?>... orders) {

        return querydslExecutor.findAll(orders);
    }

    @Override
    public Page<T> findAll(Predicate predicate, org.springframework.data.domain.Pageable pageable) {

        return querydslExecutor.findAll(predicate, pageable);
    }

    @Override
    public long count(Predicate predicate) {

        return querydslExecutor.count(predicate);
    }

    @Override
    public boolean exists(Predicate predicate) {

        return querydslExecutor.exists(predicate);
    }

    @Override
    public <S extends T, R> R findBy(Predicate predicate, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {

        return querydslExecutor.findBy(predicate, queryFunction);
    }

}
