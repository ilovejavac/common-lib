package com.dev.lib.jpa.entity.batch;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.QueryContext;
import com.dev.lib.jpa.entity.delete.CascadeFieldResolver;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        if (CascadeFieldResolver.hasCascadeFields(repository)) {
            T managed = repository.getEntityManager().find(repository.getEntityClass(), id);
            if (managed != null) {
                cascadeHardDeleteBatch(repository, List.of(managed));
                return;
            }
        }

        repository.getQueryFactory().delete(repository.getPath())
                .where(repository.getIdPath().eq(id))
                .execute();
        repository.getEntityManager().flush();
        repository.getEntityManager().clear();
    }

    public static <T extends JpaEntity> void hardDeleteAll(BaseRepositoryImpl<T> repository, Iterable<? extends T> entities) {

        if (entities == null) {
            return;
        }

        if (CascadeFieldResolver.hasCascadeFields(repository)) {
            List<T> rootEntities = new ArrayList<>();
            for (T entity : entities) {
                if (entity != null && entity.getId() != null) {
                    rootEntities.add(entity);
                }
            }
            if (!rootEntities.isEmpty()) {
                cascadeHardDeleteBatch(repository, rootEntities);
            }
            return;
        }

        BatchHelper.forEachBatch(repository, entities, entity -> (entity != null && entity.getId() != null) ? entity.getId() : null, ids -> batchHardDeleteByIds(repository, ids));
    }

    public static <T extends JpaEntity> void hardDeleteAllById(BaseRepositoryImpl<T> repository, Iterable<Long> ids) {

        if (ids == null) {
            return;
        }

        if (CascadeFieldResolver.hasCascadeFields(repository)) {
            List<Long> idList = new ArrayList<>();
            for (Long id : ids) {
                if (id != null) {
                    idList.add(id);
                }
            }
            for (int i = 0; i < idList.size(); i += repository.getInClauseBatchSize()) {
                List<Long> batch = idList.subList(i, Math.min(i + repository.getInClauseBatchSize(), idList.size()));
                List<T> loaded = repository.getQueryFactory().selectFrom(repository.getPath())
                        .where(repository.getIdPath().in(batch))
                        .fetch();
                if (!loaded.isEmpty()) {
                    cascadeHardDeleteBatch(repository, loaded);
                }
            }
            return;
        }

        BatchHelper.forEachBatch(repository, ids, id -> id, batch -> batchHardDeleteByIds(repository, batch));
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
            List<Long> ids = BatchHelper.fetchIdsAfter(repository, predicate, lastId);
            if (ids.isEmpty()) {
                break;
            }

            if (CascadeFieldResolver.hasCascadeFields(repository)) {
                List<T> loaded = repository.getQueryFactory().selectFrom(repository.getPath())
                        .where(repository.getIdPath().in(ids))
                        .fetch();
                if (!loaded.isEmpty()) {
                    cascadeHardDeleteBatch(repository, loaded);
                }
                totalAffected += ids.size();
            } else {
                long affected = repository.getQueryFactory().delete(repository.getPath())
                        .where(repository.getIdPath().in(ids))
                        .execute();
                totalAffected += affected;
                repository.getEntityManager().flush();
                repository.getEntityManager().clear();
            }

            lastId = ids.getLast();
        }

        return totalAffected;
    }

    private static <T extends JpaEntity> void cascadeHardDeleteBatch(BaseRepositoryImpl<T> repository, List<T> rootEntities) {

        Map<Class<?>, Set<Long>> toDeleteByType = CascadeFieldResolver.newDeleteByTypeMap();
        Set<Object> visited = CascadeFieldResolver.newVisitedSet();

        for (T entity : rootEntities) {
            CascadeFieldResolver.collectCascadeEntitiesIncludeDeleted(entity, visited, toDeleteByType);
        }

        // Reverse order: delete children first, then parents
        List<Map.Entry<Class<?>, Set<Long>>> entries = new ArrayList<>(toDeleteByType.entrySet());
        Collections.reverse(entries);

        CascadeFieldResolver.executeByType(
                entries,
                repository.getInClauseBatchSize(),
                (builder, builderIdPath, batch) -> repository.getQueryFactory().delete(builder)
                        .where(builderIdPath.in(batch))
                        .execute()
        );

        repository.getEntityManager().flush();
        repository.getEntityManager().clear();
    }

    private static <T extends JpaEntity> void batchHardDeleteByIds(BaseRepositoryImpl<T> repository, List<Long> ids) {

        repository.getQueryFactory().delete(repository.getPath())
                .where(repository.getIdPath().in(ids))
                .execute();
        repository.getEntityManager().flush();
        repository.getEntityManager().clear();
    }

    private static void flushAndDetachEntities(BaseRepositoryImpl<?> repository, List<?> managedEntities) {

        repository.getEntityManager().flush();
        for (Object entity : managedEntities) {
            if (entity != null) {
                repository.getEntityManager().detach(entity);
            }
        }
        managedEntities.clear();
    }
}
