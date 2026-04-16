package com.dev.lib.jpa.entity.batch;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.QueryContext;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class BatchOperationSupport {

    private BatchOperationSupport() {
    }

    public static <T extends JpaEntity, S extends T> List<S> saveAll(BaseRepositoryImpl<T> repository, Iterable<S> entities) {

        if (entities == null) {
            return Collections.emptyList();
        }

        List<S> result = new ArrayList<>();
        List<S> managedBatch = new ArrayList<>(repository.getJdbcBatchSize());
        for (S entity : entities) {
            if (entity == null) {
                continue;
            }
            S managed;
            if (entity.isNew()) {
                repository.getEntityManager().persist(entity);
                managed = entity;
            } else {
                managed = repository.getEntityManager().merge(entity);
            }
            result.add(managed);
            managedBatch.add(managed);
            if (managedBatch.size() >= repository.getJdbcBatchSize()) {
                flushAndDetachEntities(repository, managedBatch);
            }
        }

        if (!managedBatch.isEmpty()) {
            flushAndDetachEntities(repository, managedBatch);
        }

        return result;
    }

    public static <T extends JpaEntity> void hardDelete(BaseRepositoryImpl<T> repository, T entity) {

        if (entity == null || entity.getId() == null) {
            return;
        }
        hardDeleteById(repository, entity.getId());
    }

    public static <T extends JpaEntity> void hardDeleteById(BaseRepositoryImpl<T> repository, Long id) {

        if (id == null) {
            return;
        }

        repository.getQueryFactory().delete(repository.getPath())
                .where(repository.getIdPath().eq(id))
                .execute();
        flushAndDetachByIds(repository, List.of(id));
    }

    public static <T extends JpaEntity> void hardDeleteAll(BaseRepositoryImpl<T> repository, Iterable<? extends T> entities) {

        if (entities == null) {
            return;
        }
        forEachBatch(repository, entities, entity -> (entity != null && entity.getId() != null) ? entity.getId() : null, ids -> batchHardDeleteByIds(repository, ids));
    }

    public static <T extends JpaEntity> void hardDeleteAllById(BaseRepositoryImpl<T> repository, Iterable<Long> ids) {

        if (ids == null) {
            return;
        }
        forEachBatch(repository, ids, id -> id, batch -> batchHardDeleteByIds(repository, batch));
    }

    public static <T extends JpaEntity> long hardDelete(
            BaseRepositoryImpl<T> repository,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        QueryContext ctx = new QueryContext().withDeleted();
        long totalAffected = 0;
        Predicate predicate = RepositoryPredicateSupport.buildPredicate(
                repository.getPathBuilder(),
                repository.getPath(),
                repository.getDeletedPath(),
                ctx,
                dslQuery,
                expressions
        );

        Long lastId = null;
        while (true) {
            List<Long> ids = fetchIdsAfter(repository, predicate, lastId);
            if (ids.isEmpty()) {
                break;
            }

            long affected = repository.getQueryFactory().delete(repository.getPath())
                    .where(repository.getIdPath().in(ids))
                    .execute();

            totalAffected += affected;
            flushAndDetachByIds(repository, ids);
            lastId = ids.getLast();
        }

        return totalAffected;
    }

    private static <T extends JpaEntity> void batchHardDeleteByIds(BaseRepositoryImpl<T> repository, List<Long> ids) {

        repository.getQueryFactory().delete(repository.getPath())
                .where(repository.getIdPath().in(ids))
                .execute();
        flushAndDetachByIds(repository, ids);
    }

    private static void flushAndDetachEntities(BaseRepositoryImpl<?> repository, List<?> managedEntities) {

        repository.getEntityManager().flush();
        for (Object entity : managedEntities) {
            if (entity != null && repository.getEntityManager().contains(entity)) {
                repository.getEntityManager().detach(entity);
            }
        }
        managedEntities.clear();
    }

    private static <T extends JpaEntity> void flushAndDetachByIds(BaseRepositoryImpl<T> repository, List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return;
        }
        repository.getEntityManager().flush();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            T reference = repository.getEntityManager().getReference(repository.getEntityClass(), id);
            if (repository.getEntityManager().contains(reference)) {
                repository.getEntityManager().detach(reference);
            }
        }
    }

    private static <T extends JpaEntity> List<Long> fetchIdsAfter(BaseRepositoryImpl<T> repository, Predicate basePredicate, Long lastId) {

        BooleanBuilder where = new BooleanBuilder(basePredicate);
        if (lastId != null) {
            where.and(repository.getIdPath().gt(lastId));
        }
        return repository.getQueryFactory().select(repository.getIdPath())
                .from(repository.getPath())
                .where(where)
                .orderBy(repository.getIdPath().asc())
                .limit(repository.getInClauseBatchSize())
                .fetch();
    }

    private static <T extends JpaEntity, E> void forEachBatch(
            BaseRepositoryImpl<T> repository,
            Iterable<? extends E> source,
            Function<E, Long> idExtractor,
            java.util.function.Consumer<List<Long>> batchAction
    ) {

        List<Long> batch = new ArrayList<>(repository.getInClauseBatchSize() + 1);
        for (E item : source) {
            Long id = idExtractor.apply(item);
            if (id == null) {
                continue;
            }
            batch.add(id);
            if (batch.size() >= repository.getInClauseBatchSize()) {
                batchAction.accept(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            batchAction.accept(batch);
        }
    }
}
