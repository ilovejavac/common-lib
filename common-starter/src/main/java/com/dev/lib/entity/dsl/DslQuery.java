package com.dev.lib.entity.dsl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DslQuery<T> {
    private static final Map<Class<?>, EntityPathBase<?>> ENTITY_PATH_CACHE = new ConcurrentHashMap<>();

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final EntityPathBase<T> entityPath;

    @SuppressWarnings("unchecked")
    protected DslQuery() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            Class<T> entityClass = (Class<T>) parameterizedType.getActualTypeArguments()[0];
            this.entityPath = (EntityPathBase<T>) ENTITY_PATH_CACHE.computeIfAbsent(
                    entityClass,
                    this::createEntityPath
            );
        } else {
            throw new IllegalStateException("必须指定泛型类型");
        }
    }

    @SuppressWarnings("unchecked")
    private EntityPathBase<T> createEntityPath(Class<?> entityClass) {
        try {
            String qClassName = entityClass.getPackage().getName() + ".Q" + entityClass.getSimpleName();
            Class<?> qClass = Class.forName(qClassName);

            String fieldName = Character.toLowerCase(entityClass.getSimpleName().charAt(0))
                    + entityClass.getSimpleName().substring(1);

            Field field = qClass.getDeclaredField(fieldName);
            ReflectionUtils.makeAccessible(field);

            return (EntityPathBase<T>) field.get(null);
        } catch (Exception e) {
            throw new IllegalStateException("无法创建 Q 类实例: " + entityClass.getName(), e);
        }
    }

    /**
     * 构建 Predicate
     */
    public BooleanBuilder toPredicate() {
        BooleanBuilder builder = new BooleanBuilder();

        PathBuilder<T> pathBuilder = new PathBuilder<>(
                entityPath.getType(),
                entityPath.getMetadata()
        );

        Arrays.stream(getClass().getDeclaredFields())
                .forEach(field -> processField(field, pathBuilder, builder));

        return builder;
    }

    private void processField(Field field, PathBuilder<T> pathBuilder, BooleanBuilder builder) {
        Condition condition = field.getAnnotation(Condition.class);
        ReflectionUtils.makeAccessible(field);

        Object value;
        try {
            value = field.get(this);
        } catch (IllegalAccessException e) {
            return;
        }

        if (value == null) {
            return;
        }

        String targetField;
        if (condition == null) {
            targetField = field.getName();
        } else {
            targetField = condition.field().isEmpty() ?
                    field.getName() : condition.field();
        }

        BooleanExpression expression = buildExpression(
                pathBuilder,
                targetField,
                Optional.ofNullable(condition).map(Condition::type).orElse(QueryType.EQ),
                value
        );

        if (expression != null) {
            builder.and(expression);
        }
    }

    private BooleanExpression buildExpression(
            PathBuilder<T> pathBuilder,
            String field,
            QueryType type,
            Object value
    ) {

        // 处理嵌套字段：user.profile.name
        PathBuilder<?> currentPath = pathBuilder;
        String[] fieldParts = field.split("\\.");

        for (int i = 0; i < fieldParts.length - 1; i++) {
            currentPath = currentPath.get(fieldParts[i]);
        }

        String finalField = fieldParts[fieldParts.length - 1];
        return switch (type) {
            case EQ -> eq(currentPath, finalField, value);
            case NE -> ne(currentPath, finalField, value);
            case GT -> gt(currentPath, finalField, value);
            case GE -> goe(currentPath, finalField, value);
            case LT -> lt(currentPath, finalField, value);
            case LE -> loe(currentPath, finalField, value);
            case LIKE -> like(currentPath, finalField, value);
            case START_WITH -> startWith(currentPath, finalField, value);
            case END_WITH -> endWith(currentPath, finalField, value);
            case IN -> in(currentPath, finalField, value);
            case NOT_IN -> notIn(currentPath, finalField, value);
            case IS_NULL -> currentPath.get(finalField).isNull();
            case IS_NOT_NULL -> currentPath.get(finalField).isNotNull();
            default -> null;
        };
    }

// ========== 辅助方法 ==========

    private BooleanExpression eq(PathBuilder<?> path, String field, Object value) {
        return path.get(field).eq(value);
    }

    private BooleanExpression ne(PathBuilder<?> path, String field, Object value) {
        return path.get(field).ne(value);
    }

    @SuppressWarnings("unchecked")
    private BooleanExpression gt(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("GT 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .gt((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private BooleanExpression goe(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("GE 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .goe((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private BooleanExpression lt(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("LT 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .lt((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private BooleanExpression loe(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("LE 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .loe((Comparable<?>) value);
    }

    private BooleanExpression like(PathBuilder<?> path, String field, Object value) {
        return path.getString(field).containsIgnoreCase(value.toString());
    }

    private BooleanExpression startWith(PathBuilder<?> path, String field, Object value) {
        return path.getString(field).startsWithIgnoreCase(value.toString());
    }

    private BooleanExpression endWith(PathBuilder<?> path, String field, Object value) {
        return path.getString(field).endsWithIgnoreCase(value.toString());
    }


    private BooleanExpression in(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("IN 操作要求值必须是 Collection 类型");
        }
        if (CollectionUtils.isEmpty((Collection<?>) value)) {
            return null;
        }
        return path.get(field).in((Collection<?>) value);
    }

    private BooleanExpression notIn(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("NOT_IN 操作要求值必须是 Collection 类型");
        }
        if (CollectionUtils.isEmpty((Collection<?>) value)) {
            return null;
        }

        return path.get(field).notIn((Collection<?>) value);
    }
}
