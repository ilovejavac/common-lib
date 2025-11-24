package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.ConditionIgnore;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

                            boolean isNestedQuery = DslQuery.class.isAssignableFrom(field.getType());

                            fieldMetas.add(new FieldMeta(
                                    field,
                                    condition,
                                    condition != null && StringUtils.hasText(condition.field())
                                            ? condition.field()
                                            : parsedField.targetField(),
                                    condition != null ? condition.type() : parsedField.queryType(),
                                    condition != null ? condition.operator() : LogicalOperator.AND,
                                    isNestedQuery
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

    @SuppressWarnings("unchecked")
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

    @Getter
    public static class ClassMeta {
        private final Class<?> entityClass;
        private final List<FieldMeta> fields;

        ClassMeta(Class<?> entityClass, List<FieldMeta> fields) {
            this.entityClass = entityClass;
            this.fields = fields;
        }
    }

    @Getter
    public static class FieldMeta {
        private final Field field;
        private final Condition condition;
        private final String targetField;
        private final QueryType queryType;
        private final LogicalOperator operator;
        private final boolean nestedQuery;

        FieldMeta(
                Field field, Condition condition, String targetField,
                QueryType queryType, LogicalOperator operator, boolean nestedQuery
        ) {
            this.field = field;
            this.condition = condition;
            this.targetField = targetField;
            this.queryType = queryType;
            this.operator = operator;
            this.nestedQuery = nestedQuery;
        }

        public Object getValue(Object instance) {
            try {
                return ReflectionUtils.getField(field, instance);
            } catch (Exception e) {
                return null;
            }
        }
    }
}