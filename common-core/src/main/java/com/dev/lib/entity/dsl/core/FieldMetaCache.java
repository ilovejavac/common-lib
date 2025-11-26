package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.ConditionIgnore;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FieldMetaCache {
    private FieldMetaCache() {
    }

    private static final Map<Class<?>, ClassMeta> CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<FieldMeta>> FIELD_META_CACHE = new ConcurrentHashMap<>();

    public static ClassMeta getMeta(Class<?> queryClass) {
        return CACHE.computeIfAbsent(queryClass, FieldMetaCache::buildMeta);
    }

    private static ClassMeta buildMeta(Class<?> queryClass) {
        Class<?> entityClass = resolveEntityClass(queryClass);
        return new ClassMeta(entityClass, resolveFieldMeta(queryClass));
    }

    public static List<FieldMeta> resolveFieldMeta(Class<?> target) {
        return FIELD_META_CACHE.computeIfAbsent(
                target, k -> {
                    List<FieldMeta> fieldMetas = new ArrayList<>();

                    Class<?> current = target;
                    while (current != null && current != Object.class) {
                        for (Field field : current.getDeclaredFields()) {
                            if (shouldSkip(field)) continue;

                            ReflectionUtils.makeAccessible(field);
                            Condition condition = field.getAnnotation(Condition.class);
                            QueryFieldParser.ParsedField parsedField = QueryFieldParser.parse(field.getName());

                            QueryType queryType =
                                    Optional.ofNullable(condition).map(Condition::type).orElse(parsedField.queryType());
                            fieldMetas.add(new FieldMeta(
                                    field,
                                    condition,
                                    condition != null && StringUtils.hasText(condition.field())
                                            ? condition.field()
                                            : parsedField.targetField(),
                                    queryType.equals(QueryType.EMPTY) ?
                                            parsedField.queryType() : queryType,
                                    condition != null ? condition.operator() : LogicalOperator.AND,
                                    false
                            ));
                        }
                        current = current.getSuperclass();
                    }

                    return fieldMetas;
                }
        );
    }

    private static boolean shouldSkip(Field field) {
        return field.isAnnotationPresent(JsonIgnore.class)
                || field.isAnnotationPresent(ConditionIgnore.class)
                || field.getName().equals("entityPath")
                || field.getName().equals("externalFields")
                || field.getName().equals("pageRequest")
                || field.getName().equals("selfOperator")
                || Modifier.isStatic(field.getModifiers());
    }

    private static Class<?> resolveEntityClass(Class<?> queryClass) {
        Class<?> current = queryClass;
        while (current != null && current != Object.class) {
            Type superclass = current.getGenericSuperclass();
            if (superclass instanceof ParameterizedType pt) {
                Type rawType = pt.getRawType();
                if (rawType == DslQuery.class ||
                        (rawType instanceof Class && DslQuery.class.isAssignableFrom((Class<?>) rawType))) {
                    return (Class<?>) pt.getActualTypeArguments()[0];
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("无法解析泛型类型: " + queryClass.getName());
    }

    public record ClassMeta(Class<?> entityClass, List<FieldMeta> fields) {
    }

    public record FieldMeta(Field field, Condition condition, String targetField, QueryType queryType,
                            LogicalOperator operator, boolean nestedQuery) {

        public Object getValue(Object instance) {
            try {
                return ReflectionUtils.getField(field, instance);
            } catch (Exception e) {
                return null;
            }
        }
    }
}