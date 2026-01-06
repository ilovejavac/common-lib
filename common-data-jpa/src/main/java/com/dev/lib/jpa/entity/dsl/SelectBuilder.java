package com.dev.lib.jpa.entity.dsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class SelectBuilder<T> {

    private static final Map<Class<?>, ClassFieldCache> FIELD_CACHE = new ConcurrentHashMap<>(256);

    private final Class<T> entityClass;
    private final List<FieldSelector<T>> selectors = new ArrayList<>();
    private final Map<String, Expression<?>> pathExpressions = new LinkedHashMap<>();

    @SafeVarargs
    public SelectBuilder(Class<T> entityClass, SFunction<T, ?>... fields) {
        this.entityClass = entityClass;
        for (SFunction<T, ?> fn : fields) {
            selectors.add(FieldSelector.of(fn));
        }
    }

    public SelectBuilder(Class<T> entityClass, String... fieldNames) {
        this.entityClass = entityClass;
        for (String name : fieldNames) {
            selectors.add(new FieldSelector<>(name, Object.class, List.of()));
        }
    }

    public SelectBuilder(Class<T> entityClass, List<FieldSelector<T>> selectors) {
        this.entityClass = entityClass;
        this.selectors.addAll(selectors);
    }

    public boolean isEmpty() {
        return selectors.isEmpty();
    }

    public Expression<?>[] buildExpressions(PathBuilder<?> rootPath) {
        pathExpressions.clear();

        for (FieldSelector<T> selector : selectors) {
            String path = selector.getPath();
            Expression<?> expr = buildPathExpression(rootPath, path);
            pathExpressions.put(path, expr);
        }

        return pathExpressions.values().toArray(Expression[]::new);
    }

    private Expression<?> buildPathExpression(PathBuilder<?> rootPath, String path) {
        String[] parts = path.split("\\.");
        PathBuilder<?> current = rootPath;

        for (int i = 0; i < parts.length - 1; i++) {
            current = current.get(parts[i]);
        }

        return current.get(parts[parts.length - 1]);
    }

    public T toEntity(Tuple tuple) {
        return toObject(tuple, entityClass);
    }

    public <D> D toDto(Tuple tuple, Class<D> dtoClass) {
        return toObject(tuple, dtoClass);
    }

    @SuppressWarnings("unchecked")
    private <D> D toObject(Tuple tuple, Class<D> targetClass) {
        ClassFieldCache cache = getFieldCache(targetClass);

        // 收集值
        Map<String, Object> values = new LinkedHashMap<>();
        Expression<?>[] expressions = pathExpressions.values().toArray(Expression[]::new);

        int index = 0;
        for (FieldSelector<T> selector : selectors) {
            String path = selector.getPath();
            Object value = tuple.get(expressions[index]);
            if (value != null) {
                values.put(path, value);
            }
            index++;
        }

        // 尝试无参构造 + 反射赋值
        D instance = tryCreateWithNoArg(targetClass, cache, values);
        if (instance != null) {
            return instance;
        }

        // 尝试全参构造（Kotlin data class）
        return createWithAllArgs(targetClass, cache, values);
    }

    private <D> D tryCreateWithNoArg(Class<D> targetClass, ClassFieldCache cache, Map<String, Object> values) {
        try {
            Constructor<D> constructor = targetClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            D instance = constructor.newInstance();

            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String path = entry.getKey();
                Object value = entry.getValue();

                // 1. 简单名匹配: name, description
                String simpleName = path.contains(".")
                                    ? path.substring(path.lastIndexOf('.') + 1)
                                    : path;
                Field field = cache.getField(simpleName);

                // 2. 扁平化匹配: profile.name -> profileName
                if (field == null && path.contains(".")) {
                    String flatName = toFlatFieldName(path);
                    field = cache.getField(flatName);
                }

                // 3. 嵌套赋值
                if (field == null && path.contains(".")) {
                    setNestedValue(instance, cache, path, value);
                    continue;
                }

                if (field != null) {
                    field.set(instance, convertValue(value, field.getType()));
                }
            }

            return instance;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("对象创建失败: " + targetClass.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <D> D createWithAllArgs(Class<D> targetClass, ClassFieldCache cache, Map<String, Object> values) {
        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();

        // 找参数最多的构造函数
        Constructor<?> bestConstructor = Arrays.stream(constructors)
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new IllegalStateException("找不到构造函数: " + targetClass.getName()));

        bestConstructor.setAccessible(true);

        java.lang.reflect.Parameter[] params = bestConstructor.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            String paramName = params[i].getName();
            Class<?> paramType = params[i].getType();

            // 查找匹配的值
            Object value = findValueForParam(paramName, values);
            args[i] = convertValue(value, paramType);
        }

        try {
            return (D) bestConstructor.newInstance(args);
        } catch (Exception e) {
            throw new IllegalStateException("构造函数调用失败: " + targetClass.getName(), e);
        }
    }

    private Object findValueForParam(String paramName, Map<String, Object> values) {
        // 1. 精确匹配 path 的最后一段
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String path = entry.getKey();
            String simpleName = path.contains(".")
                                ? path.substring(path.lastIndexOf('.') + 1)
                                : path;
            if (simpleName.equals(paramName)) {
                return entry.getValue();
            }
        }

        // 2. 扁平化匹配
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String flatName = toFlatFieldName(entry.getKey());
            if (flatName.equals(paramName)) {
                return entry.getValue();
            }
        }

        return null;
    }

    private void setNestedValue(Object target, ClassFieldCache rootCache, String path, Object value) {
        String[] parts = path.split("\\.");
        Object current = target;
        ClassFieldCache currentCache = rootCache;

        for (int i = 0; i < parts.length - 1; i++) {
            String fieldName = parts[i];
            Field field = currentCache.getField(fieldName);
            if (field == null) return;

            try {
                Object next = field.get(current);
                if (next == null) {
                    next = field.getType().getDeclaredConstructor().newInstance();
                    field.set(current, next);
                }
                current = next;
                currentCache = getFieldCache(field.getType());
            } catch (Exception e) {
                return;
            }
        }

        Field finalField = currentCache.getField(parts[parts.length - 1]);
        if (finalField != null) {
            try {
                finalField.set(current, convertValue(value, finalField.getType()));
            } catch (Exception ignored) {
            }
        }
    }

    private String toFlatFieldName(String path) {
        if (!path.contains(".")) return path;

        String[] parts = path.split("\\.");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return getDefaultValue(targetType);
        }
        if (targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) return value.toString();
        if ((targetType == Long.class || targetType == long.class) && value instanceof Number n)
            return n.longValue();
        if ((targetType == Integer.class || targetType == int.class) && value instanceof Number n)
            return n.intValue();
        if ((targetType == Double.class || targetType == double.class) && value instanceof Number n)
            return n.doubleValue();
        if ((targetType == Float.class || targetType == float.class) && value instanceof Number n)
            return n.floatValue();
        if ((targetType == Boolean.class || targetType == boolean.class) && value instanceof Boolean)
            return value;
        if ((targetType == Short.class || targetType == short.class) && value instanceof Number n)
            return n.shortValue();
        if ((targetType == Byte.class || targetType == byte.class) && value instanceof Number n)
            return n.byteValue();

        return value;
    }

    private Object getDefaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        if (type == char.class) return '\0';
        return null;
    }

    private static ClassFieldCache getFieldCache(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, ClassFieldCache::new);
    }

    // ==================== 字段缓存 ====================

    private static class ClassFieldCache {
        private final Map<String, Field> fieldMap = new HashMap<>();

        ClassFieldCache(Class<?> clazz) {
            for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    f.setAccessible(true);
                    fieldMap.putIfAbsent(f.getName(), f);
                }
            }
        }

        Field getField(String name) {
            return fieldMap.get(name);
        }
    }
}
