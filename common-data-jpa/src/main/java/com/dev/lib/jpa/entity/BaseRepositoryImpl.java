package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.util.StringUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
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

    /**
     * 级联删除字段缓存：Class -> 需要级联删除的字段列表
     */
    private static final Map<Class<?>, List<Field>> CASCADE_FIELDS_CACHE = new ConcurrentHashMap<>();

    private final EntityManager entityManager;

    private final JPAQueryFactory queryFactory;

    private final EntityPath<T> path;

    private final PathBuilder<T> pathBuilder;

    private final QuerydslJpaPredicateExecutor<T> querydslExecutor;

    private final BooleanPath deletedPath;

    /**
     * 当前实体是否有级联删除字段（缓存结果，避免重复计算）
     */
    private final boolean hasCascadeFields;

    public BaseRepositoryImpl(JpaEntityInformation<T, Long> entityInformation, EntityManager em) {

        super(entityInformation, em);
        this.entityManager = em;
        this.queryFactory = new JPAQueryFactory(em);
        this.path = SimpleEntityPathResolver.INSTANCE.createPath(entityInformation.getJavaType());
        this.pathBuilder = new PathBuilder<>(path.getType(), path.getMetadata());

        this.querydslExecutor = new QuerydslJpaPredicateExecutor<>(
                entityInformation,
                em,
                SimpleEntityPathResolver.INSTANCE,
                null
        );
        this.deletedPath = resolvePath("deleted", BooleanPath.class);
        // 初始化时计算一次，避免每次删除都重复计算
        this.hasCascadeFields = !getCascadeFields(path.getType()).isEmpty();
    }

    // ========== 查询（默认排除已删除）==========

    @Override
    public Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return load(new QueryContext(), dslQuery, expressions);
    }

    @Override
    public List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return loads(new QueryContext(), dslQuery, expressions);
    }

    @Override
    public Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return page(new QueryContext(), dslQuery, expressions);
    }

    @Override
    public boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return exists(new QueryContext(), dslQuery, expressions);
    }

    @Override
    public long count(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return count(new QueryContext(), dslQuery, expressions);
    }

    // ========== 查询（带上下文）==========

    Optional<T> load(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

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

    List<T> loads(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

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

    Page<T> page(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("分页不支持加锁");
        }
        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);
        return querydslExecutor.findAll(predicate, dslQuery.toPageable(getAllowFields(dslQuery)));
    }

    long count(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return querydslExecutor.count(buildPredicate(ctx, dslQuery, expressions));
    }

    boolean exists(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return querydslExecutor.exists(buildPredicate(ctx, dslQuery, expressions));
    }

    // ========== 逻辑删除（支持级联）==========
    private boolean isEmptyPredicate(Predicate predicate) {

        if (predicate == null) return true;
        if (predicate instanceof BooleanBuilder bb) {
            return !bb.hasValue();
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        // 1. 先校验业务条件
        Predicate businessPredicate = toPredicate(dslQuery, expressions);
        if (isEmptyPredicate(businessPredicate)) {
            throw new IllegalArgumentException("批量删除必须指定业务条件，防止误删全表");
        }

        if (hasCascadeFields) {
            // 2. 级联删除需要完整条件来查询实体
            Predicate   fullPredicate = buildPredicate(new QueryContext(), dslQuery, expressions);
            JPAQuery<T> query         = queryFactory.selectFrom(path).where(fullPredicate);
            if (dslQuery != null) {
                applySort(query, dslQuery);
            }
            List<T> entities = query.fetch();
            if (!entities.isEmpty()) {
                cascadeSoftDeleteBatch(entities);
            }
        } else {
            // 3. 非级联删除：只传业务条件，batchSoftDelete 会加 deleted=false
            batchSoftDelete(businessPredicate);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(T entity) {

        if (entity == null || entity.getId() == null) return;

        if (hasCascadeFields) {
            // 优先使用已托管的实体，避免重复查询
            T managed = entityManager.contains(entity)
                        ? entity
                        : entityManager.find(path.getType(), entity.getId());
            if (managed != null) {
                cascadeSoftDeleteBatch(List.of(managed));
                return;
            }
        }

        // 无级联或实体不存在：直接批量更新
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
                batchSoftDelete(pathBuilder.getNumber("id", Long.class).in(batch));  // ✅ 正确
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
            batchSoftDelete(null);  // ✅ 正确，batchSoftDelete 会加 deleted=false
        }
    }

    /**
     * 批量软删除
     *
     * @param businessPredicate 业务条件（不含 deleted 条件），可为 null
     */
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

    /**
     * 级联软删除（批量优化版）
     * 收集所有需要软删除的实体，按类型分组后批量 UPDATE
     */
    private static final int BATCH_SIZE = 512;

    private void cascadeSoftDeleteBatch(List<T> rootEntities) {

        // 如果实体已经加载了级联关系，直接用；否则用 fetch join 重新加载
        List<T> fullyLoadedEntities = ensureCascadeFieldsLoaded(rootEntities);

        Map<Class<?>, Set<Long>> toDeleteByType = new LinkedHashMap<>();
        Set<Object>              visited        = Collections.newSetFromMap(new IdentityHashMap<>());

        for (T entity : fullyLoadedEntities) {
            collectCascadeEntities(entity, visited, toDeleteByType);
        }

        // 按类型批量更新
        LocalDateTime now        = LocalDateTime.now();
        Long          modifierId = SecurityContextHolder.getUserId();

        for (Map.Entry<Class<?>, Set<Long>> entry : toDeleteByType.entrySet()) {
            Class<?>   entityClass = entry.getKey();
            List<Long> ids         = new ArrayList<>(entry.getValue());

            if (ids.isEmpty()) continue;

            PathBuilder<?> builder = createPathBuilder(entityClass);

            // 分批更新
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

    /**
     * 确保级联字段已加载，避免 N+1
     */
    private List<T> ensureCascadeFieldsLoaded(List<T> entities) {

        if (entities.isEmpty()) return entities;

        // 检查是否需要重新加载
        boolean needsReload = entities.stream().anyMatch(this::hasUninitializedCascadeFields);
        if (!needsReload) {
            return entities;
        }

        // 用 fetch join 重新加载（需要根据实际级联字段动态构建）
        List<Long> ids = entities.stream().map(JpaEntity::getId).toList();

        JPAQuery<T> query = queryFactory.selectFrom(path)
                .where(pathBuilder.getNumber("id", Long.class).in(ids));

        // 动态添加 fetch join
        List<Field> cascadeFields = getCascadeFields(path.getType());
        for (Field field : cascadeFields) {
            // 注意：这里简化处理，实际可能需要处理嵌套级联
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

    private PathBuilder<?> createPathBuilder(Class<?> entityClass) {

        String entityName   = entityClass.getSimpleName();
        String variableName = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
        return new PathBuilder<>(entityClass, variableName);
    }

    /**
     * 递归收集所有需要级联删除的实体 ID
     */
    private void collectCascadeEntities(Object entity, Set<Object> visited,
                                        Map<Class<?>, Set<Long>> toDeleteByType) {

        if (entity == null || visited.contains(entity)) return;
        visited.add(entity);

        // 获取真实类型，而非代理类
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

            // 检查是否已初始化
            if (!Hibernate.isInitialized(value)) {
                Hibernate.initialize(value);  // 显式初始化
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

    /**
     * 获取类的级联删除字段（带缓存）
     */
    private List<Field> getCascadeFields(Class<?> clazz) {

        return CASCADE_FIELDS_CACHE.computeIfAbsent(clazz, this::resolveCascadeFields);
    }

    /**
     * 解析类的级联删除字段
     */
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

    /**
     * 判断字段是否配置了级联删除
     */
    private boolean shouldCascadeRemove(Field field) {

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            if (hasCascadeRemove(oneToMany.cascade()) || oneToMany.orphanRemoval()) {
                return true;
            }
        }

        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            if (hasCascadeRemove(oneToOne.cascade()) || oneToOne.orphanRemoval()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCascadeRemove(CascadeType[] cascadeTypes) {

        for (CascadeType type : cascadeTypes) {
            if (type == CascadeType.REMOVE || type == CascadeType.ALL) {
                return true;
            }
        }
        return false;
    }

    // ========== 物理删除 ==========

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

        List<T> entities = loads(new QueryContext().withDeleted(), dslQuery, expressions);
        if (entities.isEmpty()) return 0;

        for (T entity : entities) {
            entityManager.remove(entity);
        }

        return entities.size();
    }

    // ========== Predicate ==========

    private Predicate buildPredicate(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        BooleanBuilder builder = new BooleanBuilder();
        switch (ctx.getDeletedFilter()) {
            case EXCLUDE_DELETED -> builder.and(deletedPath.eq(false));
            case ONLY_DELETED -> builder.and(deletedPath.eq(true));
            case INCLUDE_DELETED -> { /* 不设置 */ }
            default -> { /* 不设置 */ }
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
        return expressions.length == 0
               ? null
               : PredicateAssembler.assemble(null, null, expressions);
    }

    // ========== 工具 ==========

    @SuppressWarnings("unchecked")
    private <P> P resolvePath(String fieldName, Class<P> type) {

        try {
            return (P) path.getClass().getField(fieldName).get(path);
        } catch (Exception e) {
            throw new IllegalStateException("实体缺少 " + fieldName + " 字段", e);
        }
    }

    private void applySort(JPAQuery<T> query, DslQuery<T> dslQuery) {

        Sort sort = dslQuery.toSort(getAllowFields(dslQuery));
        if (sort.isUnsorted()) return;
        PathBuilder<T> builder = new PathBuilder<>(path.getType(), path.getMetadata());
        for (Sort.Order order : sort) {
            query.orderBy(new OrderSpecifier<>(
                    order.isAscending() ? Order.ASC : Order.DESC,
                    builder.getComparable(order.getProperty(), Comparable.class)
            ));
        }
    }

    private void applyLimit(JPAQuery<T> query, DslQuery<T> dslQuery) {

        if (dslQuery.getLimit() != null) query.limit(dslQuery.getLimit());
        if (dslQuery.getOffset() != null) query.offset(dslQuery.getOffset());
    }

    private Set<String> getAllowFields(DslQuery<T> dslQuery) {

        if (dslQuery == null) return Collections.emptySet();
        return Arrays.stream(FieldMetaCache.getMeta(dslQuery.getClass()).entityClass().getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    // ========== 委托 QueryDSL ==========

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

    @Override
    @Transactional(readOnly = true)
    public Stream<T> stream(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return stream(new QueryContext(), dslQuery, expressions);
    }

    Stream<T> stream(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("流式查询不支持加锁");
        }

        Predicate predicate = buildPredicate(ctx, dslQuery, expressions);

        JPAQuery<T> query = queryFactory.selectFrom(path).where(predicate);
        query.setHint(HibernateHints.HINT_FETCH_SIZE, BATCH_SIZE + BATCH_SIZE);

        if (dslQuery != null) {
            applySort(query, dslQuery);
        }

        return query.stream();
    }

}
