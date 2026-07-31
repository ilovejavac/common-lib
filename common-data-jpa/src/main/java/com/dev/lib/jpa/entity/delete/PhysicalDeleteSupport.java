package com.dev.lib.jpa.entity.delete;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import jakarta.persistence.LockModeType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class PhysicalDeleteSupport {

    private static final int DELETE_BATCH_SIZE = 1024;

    private PhysicalDeleteSupport() {
    }

    public static <T extends JpaEntity> void deleteEntity(BaseRepositoryImpl<T> repository, T entity) {

        if (entity == null) {
            return;
        }
        deleteById(repository, entity.getId());
    }

    public static <T extends JpaEntity> void deleteById(BaseRepositoryImpl<T> repository, Long id) {

        if (id == null) {
            return;
        }
        deleteIds(repository, List.of(id));
    }

    public static <T extends JpaEntity> void deleteAll(
            BaseRepositoryImpl<T> repository,
            Iterable<? extends T> entities
    ) {

        if (entities == null) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        for (T entity : entities) {
            if (entity != null && entity.getId() != null) {
                ids.add(entity.getId());
            }
        }
        deleteIds(repository, ids);
    }

    public static <T extends JpaEntity> void deleteAllById(
            BaseRepositoryImpl<T> repository,
            Iterable<Long> ids
    ) {

        if (ids == null) {
            return;
        }
        List<Long> collected = new ArrayList<>();
        for (Long id : ids) {
            if (id != null) {
                collected.add(id);
            }
        }
        deleteIds(repository, collected);
    }

    public static <T extends JpaEntity> long delete(
            BaseRepositoryImpl<T> repository,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        if (dslQuery != null && dslQuery.hasAgg()) {
            throw new IllegalStateException("检测到 agg() 聚合配置，physicalDelete 不支持聚合查询");
        }
        Predicate predicate = RepositoryPredicateSupport.buildPluginAndBusinessPredicate(
                repository.getPathBuilder(),
                repository.getPath(),
                dslQuery,
                expressions
        );

        long affected = 0L;
        Long lastId = null;
        while (true) {
            List<Long> ids = fetchIds(repository, predicate, lastId);
            if (ids.isEmpty()) {
                return affected;
            }
            affected += deleteScopedIds(repository, ids);
            lastId = ids.getLast();
        }
    }

    private static <T extends JpaEntity> void deleteIds(BaseRepositoryImpl<T> repository, List<Long> ids) {

        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(ids));
        for (int start = 0; start < distinctIds.size(); start += DELETE_BATCH_SIZE) {
            int end = Math.min(start + DELETE_BATCH_SIZE, distinctIds.size());
            List<Long> batch = distinctIds.subList(start, end);
            deleteScopedIds(repository, batch);
        }
    }

    private static <T extends JpaEntity> long deleteScopedIds(
            BaseRepositoryImpl<T> repository,
            List<Long> ids
    ) {

        if (ids.isEmpty()) {
            return 0L;
        }
        Predicate scopedPredicate = RepositoryPredicateSupport.buildPluginAndBusinessPredicate(
                repository.getPathBuilder(),
                repository.getPath(),
                null,
                repository.getIdPath().in(ids)
        );
        if (CascadeFieldResolver.getCascadeFields(repository.getEntityClass()).isEmpty()) {
            long affected = repository.getQueryFactory()
                    .delete(repository.getPath())
                    .where(scopedPredicate)
                    .execute();
            flushAndClear(repository, affected);
            return affected;
        }

        List<T> entities = repository.getQueryFactory()
                .selectFrom(repository.getPath())
                .where(scopedPredicate)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetch();
        for (T entity : entities) {
            repository.getEntityManager().remove(entity);
        }
        flushAndClear(repository, entities.size());
        return entities.size();
    }

    private static <T extends JpaEntity> List<Long> fetchIds(
            BaseRepositoryImpl<T> repository,
            Predicate predicate,
            Long lastId
    ) {

        BooleanBuilder where = new BooleanBuilder();
        if (predicate != null) {
            where.and(predicate);
        }
        if (lastId != null) {
            where.and(repository.getIdPath().gt(lastId));
        }
        return repository.getQueryFactory()
                .select(repository.getIdPath())
                .from(repository.getPath())
                .where(where)
                .orderBy(repository.getIdPath().asc())
                .limit(DELETE_BATCH_SIZE)
                .fetch();
    }

    private static void flushAndClear(BaseRepositoryImpl<?> repository, long affected) {

        if (affected == 0) {
            return;
        }
        repository.getEntityManager().flush();
        repository.getEntityManager().clear();
    }
}
