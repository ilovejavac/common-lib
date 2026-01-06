package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.jpa.entity.dsl.SelectBuilder;
import com.dev.lib.jpa.entity.dsl.plugin.QueryPluginChain;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.util.StringUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseRepositoryImpl<T extends JpaEntity> extends SimpleJpaRepository<T, Long> implements BaseRepository<T> {

    private static final int BATCH_SIZE = 512;

    private static final Map<Class<?>, List<Field>> CASCADE_FIELDS_CACHE = new ConcurrentHashMap<>(128);

    private final EntityManager entityManager;

    private final JPAQueryFactory queryFactory;

    private final EntityPath<T> path;

    private final PathBuilder<T> pathBuilder;

    private final QuerydslJpaPredicateExecutor<T> querydslExecutor;

    private final BooleanPath deletedPath;

    private final Class<T> entityClass;

    private final boolean hasCascadeFields;

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
        this.hasCascadeFields = !getCascadeFields(path.getType()).isEmpty();
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
            return (Page<D>) pageFullEntity(ctx, dslQuery, expressions);
        }

        List<D> content = doQuery(ctx, select, resultClass, dslQuery, expressions, null);
        long    total   = count(ctx, dslQuery, expressions);
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

        query.setHint(HibernateHints.HINT_FETCH_SIZE, BATCH_SIZE * 2);

        if (dslQuery != null) {
            applySort(query, dslQuery);
        }

        if (resultClass == entityClass) {
            return (Stream<D>) query.stream().map(select::toEntity);
        }
        return query.stream().map(t -> select.toDto(t, resultClass));
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

    private Stream<T> streamFullEntity(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);

        JPAQuery<T> query = queryFactory.selectFrom(path).where(predicate);
        query.setHint(HibernateHints.HINT_FETCH_SIZE, BATCH_SIZE * 2);

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
            Predicate   fullPredicate = buildPredicate(ctx, dslQuery, expressions);
            JPAQuery<T> query         = queryFactory.selectFrom(path).where(fullPredicate);
            if (dslQuery != null) {
                applySort(query, dslQuery);
            }
            List<T> entities = query.fetch();
            if (!entities.isEmpty()) {
                cascadeSoftDeleteBatch(entities);
            }
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

        List<Long> ids = new ArrayList<>();
        for (T entity : entities) {
            if (entity != null && entity.getId() != null) {
                ids.add(entity.getId());
            }
        }

        if (ids.isEmpty()) return;

        if (!hasCascadeFields) {
            for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
                List<Long> batch = ids.subList(i, Math.min(i + BATCH_SIZE, ids.size()));
                batchSoftDelete(pathBuilder.getNumber("id", Long.class).in(batch));
            }
            return;
        }

        List<T> toDelete = queryFactory.selectFrom(path)
                .where(pathBuilder.getNumber("id", Long.class).in(ids))
                .where(deletedPath.eq(false))
                .fetch();

        if (!toDelete.isEmpty()) {
            cascadeSoftDeleteBatch(toDelete);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll() {

        if (hasCascadeFields) {
            List<T> all = queryFactory.selectFrom(path)
                    .where(deletedPath.eq(false))
                    .fetch();
            if (!all.isEmpty()) {
                cascadeSoftDeleteBatch(all);
            }
        } else {
            batchSoftDelete(null);
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

            for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
                List<Long> batch = ids.subList(i, Math.min(i + BATCH_SIZE, ids.size()));

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
        for (T entity : entities) {
            hardDelete(entity);
        }
    }

    void hardDeleteAllById(Iterable<Long> ids) {

        if (ids == null) return;
        for (Long id : ids) {
            hardDeleteById(id);
        }
    }

    long hardDelete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        List<T> entities = loads(new QueryContext().withDeleted(), null, entityClass, dslQuery, expressions);
        if (entities.isEmpty()) return 0;

        for (T entity : entities) {
            entityManager.remove(entity);
        }

        return entities.size();
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