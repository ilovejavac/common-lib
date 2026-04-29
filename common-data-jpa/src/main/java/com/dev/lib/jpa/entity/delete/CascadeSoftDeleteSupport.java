package com.dev.lib.jpa.entity.delete;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.entity.QueryContext;
import com.dev.lib.jpa.entity.batch.BatchHelper;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.dev.lib.security.util.SecurityContextHolder;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.CollectionExpression;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLSubQuery;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CascadeSoftDeleteSupport {

    private CascadeSoftDeleteSupport() {
    }

    public static <T extends JpaEntity> long delete(
            BaseRepositoryImpl<T> repository,
            QueryContext ctx,
            DslQuery<T> dslQuery,
            BooleanExpression... expressions
    ) {

        Predicate businessPredicate = RepositoryPredicateSupport.toPredicate(dslQuery, expressions);
        if (RepositoryPredicateSupport.isEmptyPredicate(businessPredicate)) {
            throw new IllegalArgumentException("批量删除必须指定业务条件，防止误删全表");
        }

        if (ctx.getDeletedFilter() == QueryContext.DeletedFilter.ONLY_DELETED) {
            return 0L;
        }

        Predicate condition = activeCondition(
                repository,
                RepositoryPredicateSupport.buildPluginAndBusinessPredicate(
                        repository.getPathBuilder(),
                        repository.getPath(),
                        dslQuery,
                        expressions
                )
        );
        cascadeSoftDeleteChildren(repository, repository.getEntityClass(), repository.getPathBuilder(), condition, new HashSet<>());
        return executeSoftDeleteUpdate(repository, condition);
    }

    public static <T extends JpaEntity> void deleteEntity(BaseRepositoryImpl<T> repository, T entity) {

        Predicate identity = identityCondition(repository, entity);
        if (identity == null) {
            return;
        }
        Predicate condition = activeCondition(repository, identity);
        cascadeSoftDeleteChildren(repository, repository.getEntityClass(), repository.getPathBuilder(), condition, new HashSet<>());
        executeSoftDeleteUpdate(repository, condition);
    }

    public static <T extends JpaEntity> void deleteById(BaseRepositoryImpl<T> repository, Long id) {

        if (id == null) {
            return;
        }
        softDeleteById(repository, id);
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

    private static <T extends JpaEntity> void softDeleteById(BaseRepositoryImpl<T> repository, Long id) {

        softDeleteByIds(repository, List.of(id));
    }

    private static <T extends JpaEntity> void softDeleteByIds(BaseRepositoryImpl<T> repository, List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return;
        }
        Predicate condition = activeCondition(repository, repository.getIdPath().in(ids));
        cascadeSoftDeleteChildren(repository, repository.getEntityClass(), repository.getPathBuilder(), condition, new HashSet<>());
        executeSoftDeleteUpdate(repository, condition);
    }

    private static <T extends JpaEntity> Predicate identityCondition(BaseRepositoryImpl<T> repository, T entity) {

        if (entity == null) {
            return null;
        }
        if (entity.getId() != null) {
            return repository.getIdPath().eq(entity.getId());
        }
        String bizId = entity.getBizId();
        if (bizId != null && !bizId.isBlank()) {
            return repository.getPathBuilder().getString("bizId").eq(bizId);
        }
        return null;
    }

    private static <T extends JpaEntity> Predicate activeCondition(BaseRepositoryImpl<T> repository, Predicate condition) {

        return activeCondition(repository.getPathBuilder(), condition);
    }

    private static Predicate activeCondition(PathBuilder<?> pathBuilder, Predicate condition) {

        BooleanBuilder where = new BooleanBuilder(pathBuilder.getBoolean("deleted").eq(false));
        if (condition != null) {
            where.and(condition);
        }
        return where;
    }

    private static <T extends JpaEntity> long executeSoftDeleteUpdate(BaseRepositoryImpl<T> repository, Predicate condition) {

        return executeSoftDeleteUpdate(repository, repository.getPath(), repository.getPathBuilder(), condition);
    }

    private static long executeSoftDeleteUpdate(BaseRepositoryImpl<?> repository, EntityPath<?> path, PathBuilder<?> pathBuilder, Predicate condition) {

        long affected = repository.getQueryFactory().update(path)
                .set(pathBuilder.getBoolean("deleted"), true)
                .set(pathBuilder.getDateTime("updatedAt", LocalDateTime.class), LocalDateTime.now())
                .set(pathBuilder.getNumber("modifierId", Long.class), SecurityContextHolder.getUserId())
                .where(condition)
                .execute();

        if (affected > 0) {
            repository.getEntityManager().flush();
            repository.getEntityManager().clear();
        }
        return affected;
    }

    private static void cascadeSoftDeleteChildren(
            BaseRepositoryImpl<?> repository,
            Class<?> sourceClass,
            PathBuilder<?> sourcePath,
            Predicate sourcePredicate,
            Set<Class<?>> visiting
    ) {

        if (!JpaEntity.class.isAssignableFrom(sourceClass) || !visiting.add(sourceClass)) {
            return;
        }

        try {
            for (Field field : CascadeFieldResolver.getCascadeFields(sourceClass)) {
                Class<?> targetClass = resolveTargetEntity(field);
                if (targetClass == null || !JpaEntity.class.isAssignableFrom(targetClass)) {
                    continue;
                }

                PathBuilder<?> targetPath = CascadeFieldResolver.createPathBuilder(targetClass);
                Predicate targetRelationPredicate = buildCascadeTargetPredicate(sourcePath, sourcePredicate, field, targetClass, targetPath);
                if (targetRelationPredicate == null) {
                    continue;
                }

                Predicate targetPredicate = activeCondition(targetPath, targetRelationPredicate);
                cascadeSoftDeleteChildren(repository, targetClass, targetPath, targetPredicate, visiting);
                executeSoftDeleteUpdate(repository, targetPath, targetPath, targetPredicate);
            }
        } finally {
            visiting.remove(sourceClass);
        }
    }

    private static Predicate buildCascadeTargetPredicate(
            PathBuilder<?> sourcePath,
            Predicate sourcePredicate,
            Field field,
            Class<?> targetClass,
            PathBuilder<?> targetPath
    ) {

        if (Collection.class.isAssignableFrom(field.getType())) {
            return collectionTargetPredicate(sourcePath, sourcePredicate, field, targetClass, targetPath);
        }

        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null && oneToOne.mappedBy() != null && !oneToOne.mappedBy().isBlank()) {
            return inverseSingleTargetPredicate(sourcePath, sourcePredicate, oneToOne.mappedBy(), targetPath);
        }

        if (field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(ManyToOne.class)) {
            return owningSingleTargetPredicate(sourcePath, sourcePredicate, field.getName(), targetClass, targetPath);
        }

        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Predicate collectionTargetPredicate(
            PathBuilder<?> sourcePath,
            Predicate sourcePredicate,
            Field field,
            Class<?> targetClass,
            PathBuilder<?> targetPath
    ) {

        PathBuilder<?> targetAlias = new PathBuilder(targetClass, "__cascade_" + field.getName());
        CollectionExpression collectionPath = collectionPath(sourcePath, field, targetClass);
        JPQLSubQuery<Long> targetIds = JPAExpressions
                .select(targetAlias.getNumber("id", Long.class))
                .from(sourcePath)
                .join(collectionPath, (Path) targetAlias)
                .where(sourcePredicate);

        return targetPath.getNumber("id", Long.class).in(targetIds);
    }

    private static Predicate owningSingleTargetPredicate(
            PathBuilder<?> sourcePath,
            Predicate sourcePredicate,
            String fieldName,
            Class<?> targetClass,
            PathBuilder<?> targetPath
    ) {

        PathBuilder<?> relationPath = sourcePath.get(fieldName, targetClass);
        NumberPath<Long> relationId = relationPath.getNumber("id", Long.class);
        JPQLSubQuery<Long> targetIds = JPAExpressions
                .select(relationId)
                .from(sourcePath)
                .where(sourcePredicate, relationId.isNotNull());

        return targetPath.getNumber("id", Long.class).in(targetIds);
    }

    private static Predicate inverseSingleTargetPredicate(
            PathBuilder<?> sourcePath,
            Predicate sourcePredicate,
            String mappedBy,
            PathBuilder<?> targetPath
    ) {

        NumberPath<Long> sourceId = sourcePath.getNumber("id", Long.class);
        JPQLSubQuery<Long> sourceIds = JPAExpressions
                .select(sourceId)
                .from(sourcePath)
                .where(sourcePredicate);

        return targetPath.get(mappedBy, sourcePath.getType())
                .getNumber("id", Long.class)
                .in(sourceIds);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static CollectionExpression collectionPath(PathBuilder<?> sourcePath, Field field, Class<?> targetClass) {

        return sourcePath.getCollection(field.getName(), (Class) targetClass);
    }

    private static Class<?> resolveTargetEntity(Field field) {

        Class<?> fieldType = field.getType();
        if (!Collection.class.isAssignableFrom(fieldType)) {
            return fieldType;
        }

        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?> targetClass) {
                return targetClass;
            }
        }
        return null;
    }
}
