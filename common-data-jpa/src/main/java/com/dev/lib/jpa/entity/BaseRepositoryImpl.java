package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.util.StringUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.QuerydslJpaPredicateExecutor;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BaseRepositoryImpl<T extends JpaEntity> extends SimpleJpaRepository<T, Long> implements BaseRepository<T> {

    private final EntityManager entityManager;

    private final JPAQueryFactory queryFactory;

    private final EntityPath<T> path;

    private final QuerydslJpaPredicateExecutor<T> querydslExecutor;

    private final BooleanPath deletedPath;

//    private final NumberPath<Long> idPath;

    public BaseRepositoryImpl(JpaEntityInformation<T, Long> entityInformation, EntityManager em) {

        super(
                entityInformation,
                em
        );
        this.entityManager = em;
        this.queryFactory = new JPAQueryFactory(em);
        this.path = SimpleEntityPathResolver.INSTANCE.createPath(entityInformation.getJavaType());
        this.querydslExecutor = new QuerydslJpaPredicateExecutor<>(
                entityInformation,
                em,
                SimpleEntityPathResolver.INSTANCE,
                null
        );
        this.deletedPath = resolvePath(
                "deleted",
                BooleanPath.class
        );
//        this.idPath = resolvePath(
//                "id",
//                NumberPath.class
//        );
    }

    // ========== 查询（默认排除已删除）==========

    @Override
    public Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return load(
                new QueryContext(),
                dslQuery,
                expressions
        );
    }

    @Override
    public Optional<T> load(BooleanExpression... expressions) {

        return load(
                new QueryContext(),
                null,
                expressions
        );
    }

    @Override
    public List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return loads(
                new QueryContext(),
                dslQuery,
                expressions
        );
    }

    @Override
    public List<T> loads(BooleanExpression... expressions) {

        return loads(
                new QueryContext(),
                null,
                expressions
        );
    }

    @Override
    public Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return page(
                new QueryContext(),
                dslQuery,
                expressions
        );
    }

    @Override
    public boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return exists(
                new QueryContext(),
                dslQuery,
                expressions
        );
    }

    @Override
    public long count(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return count(
                new QueryContext(),
                dslQuery,
                expressions
        );
    }

    // ========== 查询（带上下文）==========

    Optional<T> load(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(
                ctx,
                dslQuery,
                expressions
        );
        if (ctx.hasLock()) {
            List<T> result = queryFactory.selectFrom(path).where(predicate).setLockMode(ctx.getLockMode()).limit(1).fetch();
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        }
        return querydslExecutor.findOne(predicate);
    }

    List<T> loads(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(
                ctx,
                dslQuery,
                expressions
        );
        if (ctx.hasLock()) {
            JPAQuery<T> query = queryFactory.selectFrom(path).where(predicate).setLockMode(ctx.getLockMode());
            if (dslQuery != null) {
                applySort(
                        query,
                        dslQuery
                );
                applyLimit(
                        query,
                        dslQuery
                );
            }
            return query.fetch();
        }
        if (dslQuery != null && dslQuery.getLimit() != null) {
            return querydslExecutor.findAll(
                    predicate,
                    dslQuery.toPageable(getAllowFields(dslQuery))
            ).getContent();
        }
        Sort sort = dslQuery != null ? dslQuery.toSort(getAllowFields(dslQuery)) : Sort.unsorted();
        return querydslExecutor.findAll(
                predicate,
                sort
        );
    }

    Page<T> page(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        if (ctx.hasLock()) {
            throw new UnsupportedOperationException("分页不支持加锁");
        }
        Predicate predicate = buildPredicate(
                ctx,
                dslQuery,
                expressions
        );
        return querydslExecutor.findAll(
                predicate,
                dslQuery.toPageable(getAllowFields(dslQuery))
        );
    }

    long count(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return querydslExecutor.count(buildPredicate(
                ctx,
                dslQuery,
                expressions
        ));
    }

    boolean exists(QueryContext ctx, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return querydslExecutor.exists(buildPredicate(
                ctx,
                dslQuery,
                expressions
        ));
    }

    // ========== 逻辑删除（支持级联）==========

    @Override
    public void delete(T entity) {

        if (entity == null || entity.getId() == null) return;

        // 级联软删除子实体
        cascadeSoftDelete(
                entity,
                new HashSet<>()
        );

        // 软删除当前实体
        entity.setDeleted(true);
        entityManager.merge(entity);
    }

    @Override
    public void deleteById(Long id) {

        if (id == null) return;
        findById(id).ifPresent(this::delete);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {

        if (entities == null) return;
        for (T entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {

        List<T> all = queryFactory.selectFrom(path).where(deletedPath.eq(false)).fetch();
        for (T entity : all) {
            delete(entity);
        }
    }

    /**
     * 级联软删除：扫描带有 CascadeType.REMOVE 或 CascadeType.ALL 的关联字段
     */
    private void cascadeSoftDelete(Object entity, Set<Object> visited) {

        if (entity == null || visited.contains(entity)) {
            return;
        }
        visited.add(entity);

        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (shouldCascadeRemove(field)) {
                    ReflectionUtils.makeAccessible(field);
                    Object value = ReflectionUtils.getField(
                            field,
                            entity
                    );
                    softDeleteRelated(
                            value,
                            visited
                    );
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 判断字段是否配置了级联删除
     */
    private boolean shouldCascadeRemove(Field field) {

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null && hasCascadeRemove(oneToMany.cascade())) {
            return true;
        }

        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null && hasCascadeRemove(oneToOne.cascade())) {
            return true;
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

    /**
     * 软删除关联实体
     */
    private void softDeleteRelated(Object value, Set<Object> visited) {

        if (value == null) return;

        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                softDeleteSingle(
                        item,
                        visited
                );
            }
        } else {
            softDeleteSingle(
                    value,
                    visited
            );
        }
    }

    private void softDeleteSingle(Object item, Set<Object> visited) {

        if (item instanceof JpaEntity child && !Boolean.TRUE.equals(child.getDeleted())) {
            // 先递归处理子实体的级联
            cascadeSoftDelete(
                    child,
                    visited
            );
            // 再软删除当前子实体
            child.setDeleted(true);
            entityManager.merge(child);
        }
    }

    // ========== 物理删除 ==========

    void hardDelete(T entity) {

        if (entity == null || entity.getId() == null) return;
        super.delete(entity);
    }

    void hardDeleteById(Long id) {

        if (id == null) return;
        super.deleteById(id);
    }

    void hardDeleteAll(Iterable<? extends T> entities) {

        if (entities == null) return;
        List<Long> ids = StreamSupport.stream(
                        entities.spliterator(),
                        false
                )
                .filter(e -> e != null && e.getId() != null)
                .map(JpaEntity::getId)
                .toList();
        if (!ids.isEmpty()) {
            hardDeleteAllById(ids);
        }
    }

    void hardDeleteAllById(Iterable<Long> ids) {

        if (ids == null) return;
        List<Long> validIds = StreamSupport.stream(
                        ids.spliterator(),
                        false
                )
                .filter(Objects::nonNull)
                .toList();
        if (!validIds.isEmpty()) {
            super.deleteAllByIdInBatch(ids);
        }
    }

    long hardDelete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        Predicate predicate = buildPredicate(
                new QueryContext().withDeleted(),
                dslQuery,
                expressions
        );
        return predicate == null ? 0 : queryFactory.delete(path).where(predicate).execute();
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
        Predicate dsl = toPredicate(
                dslQuery,
                expressions
        );
        if (dsl != null) builder.and(dsl);
        return builder.getValue();
    }

    private static <E extends JpaEntity> Predicate toPredicate(DslQuery<E> query, BooleanExpression... expressions) {

        if (query != null) {
            List<QueryFieldMerger.FieldMetaValue>        self   = QueryFieldMerger.resolve(query);
            Map<String, QueryFieldMerger.FieldMetaValue> fields = new HashMap<>();
            for (QueryFieldMerger.FieldMetaValue fmv : self) {
                fields.put(
                        StringUtils.format(
                                "{}-{}",
                                fmv.getFieldMeta().targetField(),
                                fmv.getFieldMeta().queryType()
                        ),
                        fmv
                );
            }
            query.getExternalFields().forEach(it -> fields.put(
                    StringUtils.format(
                            "{}-{}",
                            it.getFieldMeta().targetField(),
                            it.getFieldMeta().queryType()
                    ),
                    it
            ));
            return PredicateAssembler.assemble(
                    query,
                    fields.values(),
                    expressions
            );
        }
        return expressions.length == 0
               ? null
               : PredicateAssembler.assemble(
                       null,
                       null,
                       expressions
               );
    }

    // ========== 工具 ==========

    @SuppressWarnings("unchecked")
    private <P> P resolvePath(String fieldName, Class<P> type) {

        try {
            return (P) path.getClass().getField(fieldName).get(path);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "实体缺少 " + fieldName + " 字段",
                    e
            );
        }
    }

    private void applySort(JPAQuery<T> query, DslQuery<T> dslQuery) {

        Sort sort = dslQuery.toSort(getAllowFields(dslQuery));
        if (sort.isUnsorted()) return;
        PathBuilder<T> builder = new PathBuilder<>(
                path.getType(),
                path.getMetadata()
        );
        for (Sort.Order order : sort) {
            query.orderBy(new OrderSpecifier<>(
                    order.isAscending() ? Order.ASC : Order.DESC,
                    builder.getComparable(
                            order.getProperty(),
                            Comparable.class
                    )
            ));
        }
    }

    private void applyLimit(JPAQuery<T> query, DslQuery<T> dslQuery) {

        if (dslQuery.getLimit() != null) query.limit(dslQuery.getLimit());
        if (dslQuery.getStart() != null) query.offset(dslQuery.getStart());
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

        return querydslExecutor.findAll(
                predicate,
                sort
        );
    }

    @Override
    public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

        return querydslExecutor.findAll(
                predicate,
                orders
        );
    }

    @Override
    public List<T> findAll(OrderSpecifier<?>... orders) {

        return querydslExecutor.findAll(orders);
    }

    @Override
    public Page<T> findAll(Predicate predicate, org.springframework.data.domain.Pageable pageable) {

        return querydslExecutor.findAll(
                predicate,
                pageable
        );
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

        return querydslExecutor.findBy(
                predicate,
                queryFunction
        );
    }

}