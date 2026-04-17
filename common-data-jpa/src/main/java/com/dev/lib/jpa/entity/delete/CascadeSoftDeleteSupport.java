package com.dev.lib.jpa.entity.delete;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.QueryContext;
import com.dev.lib.jpa.entity.batch.BatchHelper;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.dev.lib.security.util.SecurityContextHolder;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CascadeSoftDeleteSupport {

    private CascadeSoftDeleteSupport() {
    }

    public static <T extends JpaEntity> void delete(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        Predicate businessPredicate = RepositoryPredicateSupport.toPredicate(dslQuery, expressions);
        if (RepositoryPredicateSupport.isEmptyPredicate(businessPredicate)) {
            throw new IllegalArgumentException("批量删除必须指定业务条件，防止误删全表");
        }

        softDeleteByPredicateInChunks(
                repository,
                RepositoryPredicateSupport.buildPredicate(
                        repository.getPathBuilder(),
                        repository.getPath(),
                        repository.getDeletedPath(),
                        ctx,
                        dslQuery,
                        expressions
                )
        );
    }

    public static <T extends JpaEntity> void deleteEntity(BaseRepositoryImpl<T> repository, T entity) {

        if (entity == null || entity.getId() == null) {
            return;
        }
        softDeleteById(repository, entity.getId(), entity);
    }

    public static <T extends JpaEntity> void deleteById(BaseRepositoryImpl<T> repository, Long id) {

        if (id == null) {
            return;
        }
        softDeleteById(repository, id, null);
    }

    public static <T extends JpaEntity> void deleteAll(BaseRepositoryImpl<T> repository, Iterable<? extends T> entities) {

        BatchHelper.forEachBatch(repository, entities, entity -> (entity != null && entity.getId() != null) ? entity.getId() : null, ids -> softDeleteByIds(repository, ids));
    }

    public static <T extends JpaEntity> void deleteAllById(BaseRepositoryImpl<T> repository, Iterable<? extends Long> ids) {

        BatchHelper.forEachBatch(repository, ids, id -> id, batch -> softDeleteByIds(repository, batch));
    }

    public static <T extends JpaEntity> void deleteAll(BaseRepositoryImpl<T> repository) {

        Long lastId = null;
        while (true) {
            List<Long> ids = BatchHelper.fetchIdsAfter(repository, repository.getDeletedPath().eq(false), lastId);
            if (ids.isEmpty()) {
                break;
            }
            softDeleteByIds(repository, ids);
            lastId = ids.getLast();
        }
    }

    private static <T extends JpaEntity> void softDeleteById(BaseRepositoryImpl<T> repository, Long id, T entityHint) {

        if (CascadeFieldResolver.hasCascadeFields(repository)) {
            T managed = (entityHint != null && repository.getEntityManager().contains(entityHint))
                    ? entityHint
                    : repository.getEntityManager().find(repository.getEntityClass(), id);
            if (managed != null) {
                cascadeSoftDeleteBatch(repository, List.of(managed));
                return;
            }
        }

        softDeleteByIds(repository, List.of(id));
    }

    private static <T extends JpaEntity> void softDeleteByIds(BaseRepositoryImpl<T> repository, List<Long> ids) {

        if (!CascadeFieldResolver.hasCascadeFields(repository)) {
            BooleanBuilder where = new BooleanBuilder(repository.getDeletedPath().eq(false));
            where.and(repository.getIdPath().in(ids));
            executeSoftDeleteUpdate(repository, where);
            return;
        }

        List<T> toDelete = repository.getQueryFactory().selectFrom(repository.getPath())
                .where(repository.getIdPath().in(ids), repository.getDeletedPath().eq(false))
                .fetch();
        if (!toDelete.isEmpty()) {
            cascadeSoftDeleteBatch(repository, toDelete);
        }
    }

    private static <T extends JpaEntity> void softDeleteByPredicateInChunks(BaseRepositoryImpl<T> repository, Predicate fullPredicate) {

        Long lastId = null;
        while (true) {
            List<Long> ids = BatchHelper.fetchIdsAfter(repository, fullPredicate, lastId);
            if (ids.isEmpty()) {
                break;
            }

            softDeleteByIds(repository, ids);
            lastId = ids.getLast();
        }
    }

    private static <T extends JpaEntity> void executeSoftDeleteUpdate(BaseRepositoryImpl<T> repository, Predicate condition) {

        long affected = repository.getQueryFactory().update(repository.getPath())
                .set(repository.getPathBuilder().getBoolean("deleted"), true)
                .set(repository.getPathBuilder().getDateTime("updatedAt", LocalDateTime.class), LocalDateTime.now())
                .set(repository.getPathBuilder().getNumber("modifierId", Long.class), SecurityContextHolder.getUserId())
                .where(condition)
                .execute();

        if (affected > 0) {
            repository.getEntityManager().flush();
            repository.getEntityManager().clear();
        }
    }

    private static <T extends JpaEntity> void cascadeSoftDeleteBatch(BaseRepositoryImpl<T> repository, List<T> rootEntities) {

        Map<Class<?>, Set<Long>> toDeleteByType = CascadeFieldResolver.newDeleteByTypeMap();
        Set<Object> visited = CascadeFieldResolver.newVisitedSet();

        for (T entity : rootEntities) {
            CascadeFieldResolver.collectCascadeEntities(entity, visited, toDeleteByType);
        }

        LocalDateTime now = LocalDateTime.now();
        Long modifierId = SecurityContextHolder.getUserId();

        CascadeFieldResolver.executeByType(
                new ArrayList<>(toDeleteByType.entrySet()),
                repository.getInClauseBatchSize(),
                (builder, builderIdPath, batch) -> repository.getQueryFactory().update(builder)
                        .set(builder.getBoolean("deleted"), true)
                        .set(builder.getDateTime("updatedAt", LocalDateTime.class), now)
                        .set(builder.getNumber("modifierId", Long.class), modifierId)
                        .where(builderIdPath.in(batch), builder.getBoolean("deleted").eq(false))
                        .execute()
        );

        repository.getEntityManager().flush();
        repository.getEntityManager().clear();
    }
}
