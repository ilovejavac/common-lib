package com.dev.lib.jpa.entity.delete;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.QueryContext;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.dev.lib.security.util.SecurityContextHolder;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class CascadeSoftDeleteSupport {

    private static final Map<Class<?>, List<Field>> CASCADE_FIELDS_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, PathBuilder<?>> PATH_BUILDER_CACHE = new ConcurrentHashMap<>(128);

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

        if (hasCascadeFields(repository)) {
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
            return;
        }

        BooleanBuilder where = new BooleanBuilder();
        if (ctx.getDeletedFilter() == QueryContext.DeletedFilter.EXCLUDE_DELETED) {
            where.and(repository.getDeletedPath().eq(false));
        }
        where.and(businessPredicate);
        executeSoftDeleteUpdate(repository, where);
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

        forEachBatch(repository, entities, entity -> (entity != null && entity.getId() != null) ? entity.getId() : null, ids -> softDeleteByIds(repository, ids));
    }

    public static <T extends JpaEntity> void deleteAllById(BaseRepositoryImpl<T> repository, Iterable<? extends Long> ids) {

        forEachBatch(repository, ids, id -> id, batch -> softDeleteByIds(repository, batch));
    }

    public static <T extends JpaEntity> void deleteAll(BaseRepositoryImpl<T> repository) {

        if (!hasCascadeFields(repository)) {
            executeSoftDeleteUpdate(repository, repository.getDeletedPath().eq(false));
            return;
        }

        Long lastId = null;
        while (true) {
            List<Long> ids = fetchIdsAfter(repository, repository.getDeletedPath().eq(false), lastId);
            if (ids.isEmpty()) {
                break;
            }
            softDeleteByIds(repository, ids);
            lastId = ids.getLast();
        }
    }

    private static <T extends JpaEntity> boolean hasCascadeFields(BaseRepositoryImpl<T> repository) {

        return !getCascadeFields(repository.getPath().getType()).isEmpty();
    }

    private static <T extends JpaEntity> void softDeleteById(BaseRepositoryImpl<T> repository, Long id, T entityHint) {

        if (hasCascadeFields(repository)) {
            T managed = (entityHint != null && repository.getEntityManager().contains(entityHint))
                    ? entityHint
                    : repository.getEntityManager().find(repository.getEntityClass(), id);
            if (managed != null) {
                cascadeSoftDeleteBatch(repository, List.of(managed));
                return;
            }
        }

        executeSoftDeleteUpdate(repository, repository.getIdPath().eq(id));
    }

    private static <T extends JpaEntity> void softDeleteByIds(BaseRepositoryImpl<T> repository, List<Long> ids) {

        if (!hasCascadeFields(repository)) {
            executeSoftDeleteUpdate(repository, repository.getIdPath().in(ids));
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
            List<Long> ids = fetchIdsAfter(repository, fullPredicate, lastId);
            if (ids.isEmpty()) {
                break;
            }

            softDeleteByIds(repository, ids);
            lastId = ids.getLast();
        }
    }

    private static <T extends JpaEntity> void executeSoftDeleteUpdate(BaseRepositoryImpl<T> repository, Predicate condition) {

        BooleanBuilder where = new BooleanBuilder(repository.getDeletedPath().eq(false));
        where.and(condition);

        long affected = repository.getQueryFactory().update(repository.getPath())
                .set(repository.getPathBuilder().getBoolean("deleted"), true)
                .set(repository.getPathBuilder().getDateTime("updatedAt", LocalDateTime.class), LocalDateTime.now())
                .set(repository.getPathBuilder().getNumber("modifierId", Long.class), SecurityContextHolder.getUserId())
                .where(where)
                .execute();

        if (affected > 0) {
            repository.getEntityManager().flush();
            repository.getEntityManager().clear();
        }
    }

    private static <T extends JpaEntity> void cascadeSoftDeleteBatch(BaseRepositoryImpl<T> repository, List<T> rootEntities) {

        Map<Class<?>, Set<Long>> toDeleteByType = new LinkedHashMap<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        for (T entity : rootEntities) {
            collectCascadeEntities(entity, visited, toDeleteByType);
        }

        LocalDateTime now = LocalDateTime.now();
        Long modifierId = SecurityContextHolder.getUserId();

        for (Map.Entry<Class<?>, Set<Long>> entry : toDeleteByType.entrySet()) {
            Class<?> clazz = entry.getKey();
            List<Long> ids = new ArrayList<>(entry.getValue());
            if (ids.isEmpty()) {
                continue;
            }

            PathBuilder<?> builder = createPathBuilder(clazz);
            NumberPath<Long> builderIdPath = builder.getNumber("id", Long.class);

            for (int i = 0; i < ids.size(); i += repository.getInClauseBatchSize()) {
                List<Long> batch = ids.subList(i, Math.min(i + repository.getInClauseBatchSize(), ids.size()));
                repository.getQueryFactory().update(builder)
                        .set(builder.getBoolean("deleted"), true)
                        .set(builder.getDateTime("updatedAt", LocalDateTime.class), now)
                        .set(builder.getNumber("modifierId", Long.class), modifierId)
                        .where(builderIdPath.in(batch), builder.getBoolean("deleted").eq(false))
                        .execute();
            }
        }

        repository.getEntityManager().flush();
        repository.getEntityManager().clear();
    }

    private static void collectCascadeEntities(Object entity, Set<Object> visited, Map<Class<?>, Set<Long>> toDeleteByType) {

        if (entity == null || visited.contains(entity)) {
            return;
        }
        visited.add(entity);

        Class<?> realClass = Hibernate.getClass(entity);

        if (entity instanceof JpaEntity jpaEntity) {
            if (Boolean.TRUE.equals(jpaEntity.getDeleted())) {
                return;
            }

            Long id = jpaEntity.getId();
            if (id != null) {
                toDeleteByType
                        .computeIfAbsent(realClass, key -> new LinkedHashSet<>())
                        .add(id);
            }
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

        if (source == null) {
            return;
        }

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

    private static PathBuilder<?> createPathBuilder(Class<?> clazz) {

        return PATH_BUILDER_CACHE.computeIfAbsent(clazz, CascadeSoftDeleteSupport::newPathBuilder);
    }

    private static PathBuilder<?> newPathBuilder(Class<?> clazz) {

        String entityName = clazz.getSimpleName();
        String variableName = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
        return new PathBuilder<>(clazz, variableName);
    }

    private static List<Field> getCascadeFields(Class<?> clazz) {

        return CASCADE_FIELDS_CACHE.computeIfAbsent(clazz, CascadeSoftDeleteSupport::resolveCascadeFields);
    }

    private static List<Field> resolveCascadeFields(Class<?> clazz) {

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

    private static boolean shouldCascadeRemove(Field field) {

        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null && (hasCascadeRemove(oneToMany.cascade()) || oneToMany.orphanRemoval())) {
            return true;
        }

        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        return oneToOne != null && (hasCascadeRemove(oneToOne.cascade()) || oneToOne.orphanRemoval());
    }

    private static boolean hasCascadeRemove(CascadeType[] cascadeTypes) {

        for (CascadeType type : cascadeTypes) {
            if (type == CascadeType.REMOVE || type == CascadeType.ALL) {
                return true;
            }
        }
        return false;
    }
}
