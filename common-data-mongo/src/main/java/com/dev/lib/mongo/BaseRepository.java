package com.dev.lib.mongo;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.DslQueryFieldResolver;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.mongo.dsl.EntityPathManager;
import com.dev.lib.mongo.dsl.PredicateAssembler;
import com.dev.lib.security.util.SecurityContextHolder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.*;
import java.util.stream.StreamSupport;

@NoRepositoryBean
public interface BaseRepository<T extends MongoEntity>
        extends MongoRepository<T, Long>, QuerydslPredicateExecutor<T> {

    int BATCH_SIZE = 256;

    // ==================== 批量写入（分批）====================

    @Override
    default <S extends T> List<S> saveAll(Iterable<S> entities) {

        if (entities == null) {
            return Collections.emptyList();
        }
        List<S> result = new ArrayList<>();
        List<S> insertBatch = new ArrayList<>(BATCH_SIZE + 1);

        for (S entity : entities) {
            if (entity == null) continue;
            if (entity.isNew()) {
                insertBatch.add(entity);
                if (insertBatch.size() >= BATCH_SIZE) {
                    result.addAll(insert(insertBatch));
                    insertBatch = new ArrayList<>(BATCH_SIZE + 1);
                }
            } else {
                result.add(save(entity));
            }
        }
        if (!insertBatch.isEmpty()) {
            result.addAll(insert(insertBatch));
        }
        return result;
    }

    @Override
    default void deleteAllById(Iterable<? extends Long> ids) {

        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                deleteById(id);
            }
        }
    }

    default QueryBuilder<T> withDeleted() {

        return new QueryBuilder<>(this).withDeleted();
    }

    default QueryBuilder<T> onlyDeleted() {

        return new QueryBuilder<>(this).onlyDeleted();
    }

    // ==================== DSL 查询 ====================

    default Optional<T> load(DslQuery<T> query, BooleanExpression... expressions) {

        return load(DeletedFilter.EXCLUDE_DELETED, query, expressions);
    }

    default Optional<T> load(DeletedFilter deletedFilter, DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "load");
        Predicate predicate = toPredicate(deletedFilter, query, expressions);
        var sort = query != null ? query.toSort(getAllowedFields(query)) : org.springframework.data.domain.Sort.unsorted();
        return findBy(predicate, q -> q.sortBy(sort).first());
    }

    default List<T> loads(DslQuery<T> query, BooleanExpression... expressions) {

        return loads(DeletedFilter.EXCLUDE_DELETED, query, expressions);
    }

    default List<T> loads(DeletedFilter deletedFilter, DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "loads");
        Predicate predicate = toPredicate(deletedFilter, query, expressions);

        if (query != null && query.getLimit() != null) {
            return findAll(predicate, query.toPageable(getAllowedFields(query))).getContent();
        }

        Iterable<T> result = query != null
                             ? findAll(predicate, query.toSort(getAllowedFields(query)))
                             : findAll(predicate);

        return StreamSupport.stream(result.spliterator(), false).toList();
    }

    default Page<T> page(DslQuery<T> query, BooleanExpression... expressions) {

        return page(DeletedFilter.EXCLUDE_DELETED, query, expressions);
    }

    default Page<T> page(DeletedFilter deletedFilter, DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "page");
        return findAll(
                toPredicate(
                        deletedFilter,
                        query,
                        expressions
                ),
                resolvePageable(query)
        );
    }

    default boolean exists(DslQuery<T> query, BooleanExpression... expressions) {

        return exists(DeletedFilter.EXCLUDE_DELETED, query, expressions);
    }

    default boolean exists(DeletedFilter deletedFilter, DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "exists");
        return exists(toPredicate(
                deletedFilter,
                query,
                expressions
        ));
    }

    default long count(DslQuery<T> query, BooleanExpression... expressions) {

        return count(DeletedFilter.EXCLUDE_DELETED, query, expressions);
    }

    default long count(DeletedFilter deletedFilter, DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "count");
        return count(toPredicate(
                deletedFilter,
                query,
                expressions
        ));
    }

    default long delete(DslQuery<T> query, BooleanExpression... expressions) {

        return delete(DeletedFilter.EXCLUDE_DELETED, query, expressions);
    }

    default long delete(DeletedFilter deletedFilter, DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "delete");
        Predicate businessPredicate = toBusinessPredicate(query, expressions);
        if (isEmptyPredicate(businessPredicate)) {
            throw new IllegalArgumentException("批量删除必须指定业务条件，防止误删全表");
        }
        if (deletedFilter == DeletedFilter.ONLY_DELETED) {
            return 0L;
        }
        Predicate predicate = toPredicate(deletedFilter, query, expressions);
        List<T> entities = StreamSupport.stream(findAll(predicate).spliterator(), false).toList();
        return softDeleteEntities(entities);
    }

    @Override
    default void deleteById(Long id) {

        if (id == null) {
            return;
        }
        findById(id).ifPresent(this::delete);
    }

    @Override
    default void delete(T entity) {

        if (entity == null) {
            return;
        }
        softDeleteEntities(List.of(entity));
    }

    @Override
    default void deleteAll(Iterable<? extends T> entities) {

        if (entities == null) {
            return;
        }
        List<T> candidates = new ArrayList<>();
        for (T entity : entities) {
            if (entity != null) {
                candidates.add(entity);
            }
        }
        softDeleteEntities(candidates);
    }

    @Override
    default void deleteAll() {

        deleteAll(loads(null));
    }

    private Predicate toPredicate(DeletedFilter deletedFilter, DslQuery<T> query, BooleanExpression... expressions) {

        com.querydsl.core.BooleanBuilder builder = new com.querydsl.core.BooleanBuilder();
        Predicate deletedPredicate = deletedPredicate(deletedFilter, resolveEntityClass(query));
        if (deletedPredicate != null) {
            builder.and(deletedPredicate);
        }
        Predicate businessPredicate = toBusinessPredicate(query, expressions);
        if (businessPredicate != null) {
            builder.and(businessPredicate);
        }
        return builder.getValue();
    }

    private Predicate toBusinessPredicate(DslQuery<T> query, BooleanExpression... expressions) {

        BooleanExpression[] safeExpressions = expressions == null ? new BooleanExpression[0] : expressions;
        if (query == null) {
            return PredicateAssembler.assemble(null, null, safeExpressions);
        }

        Collection<QueryFieldMerger.FieldMetaValue> merged = DslQueryFieldResolver.resolveMerged(
                query,
                DslQueryFieldResolver.OverridePolicy.EXTERNAL_OVERRIDE_SELF
        );

        return PredicateAssembler.assemble(query, merged, safeExpressions);
    }

    private Set<String> getAllowedFields(DslQuery<T> query) {

        if (query == null) return Collections.emptySet();
        return FieldMetaCache.getMeta(query.getClass()).entityFieldNames();
    }

    private PageRequest resolvePageable(DslQuery<T> query) {

        if (query == null) {
            return PageRequest.of(0, 128, Sort.by(Sort.Order.desc("id")));
        }
        org.springframework.data.domain.Pageable pageable = query.toPageable(getAllowedFields(query));
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );
    }

    private void ensureNonAggregateQuery(DslQuery<T> query, String operation) {

        if (query != null && query.hasAgg()) {
            throw new IllegalStateException("检测到 agg() 聚合配置，" + operation + " 不支持聚合查询");
        }
    }

    private boolean isEmptyPredicate(Predicate predicate) {

        if (predicate == null) {
            return true;
        }
        if (predicate instanceof com.querydsl.core.BooleanBuilder builder) {
            return !builder.hasValue();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveEntityClass(DslQuery<T> query) {

        if (query != null) {
            return (Class<T>) FieldMetaCache.getMeta(query.getClass()).entityClass();
        }
        for (java.lang.reflect.Type type : getClass().getGenericInterfaces()) {
            Class<T> resolved = resolveEntityClass(type);
            if (resolved != null) {
                return resolved;
            }
        }
        return (Class<T>) MongoEntity.class;
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveEntityClass(java.lang.reflect.Type type) {

        if (type instanceof java.lang.reflect.ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType() instanceof Class<?> rawClass) {
                if (BaseRepository.class.equals(rawClass)) {
                    java.lang.reflect.Type entityType = parameterizedType.getActualTypeArguments()[0];
                    if (entityType instanceof Class<?> entityClass) {
                        return (Class<T>) entityClass;
                    }
                }
                for (java.lang.reflect.Type parent : rawClass.getGenericInterfaces()) {
                    Class<T> resolved = resolveEntityClass(parent);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
        } else if (type instanceof Class<?> clazz) {
            for (java.lang.reflect.Type parent : clazz.getGenericInterfaces()) {
                Class<T> resolved = resolveEntityClass(parent);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return null;
    }

    private Predicate deletedPredicate(DeletedFilter deletedFilter, Class<T> entityClass) {

        if (deletedFilter == DeletedFilter.INCLUDE_DELETED) {
            return null;
        }
        EntityPathBase<T> entityPath = EntityPathManager.getEntityPath(entityClass);
        PathBuilder<T> pathBuilder = new PathBuilder<>(entityPath.getType(), entityPath.getMetadata());
        return deletedFilter == DeletedFilter.ONLY_DELETED
               ? pathBuilder.getDateTime("deletedAt", java.time.LocalDateTime.class).isNotNull()
               : pathBuilder.getDateTime("deletedAt", java.time.LocalDateTime.class).isNull();
    }

    private long softDeleteEntities(List<T> entities) {

        if (entities == null || entities.isEmpty()) {
            return 0L;
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Long userId = SecurityContextHolder.current().getId();
        List<T> activeEntities = new ArrayList<>();
        for (T entity : entities) {
            if (entity == null || entity.getDeletedAt() != null) {
                continue;
            }
            entity.setDeletedAt(now);
            entity.setUpdatedAt(now);
            entity.setModifierId(userId);
            activeEntities.add(entity);
        }
        if (activeEntities.isEmpty()) {
            return 0L;
        }
        saveAll(activeEntities);
        return activeEntities.size();
    }

}
