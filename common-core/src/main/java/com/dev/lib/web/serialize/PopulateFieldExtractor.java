package com.dev.lib.web.serialize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 递归提取对象中所有 @PopulateField 标记字段的值
 * 按 loader 分组
 */
@Slf4j
public class PopulateFieldExtractor {

    // 缓存：Class -> List<FieldMeta>
    private static final Map<Class<?>, List<FieldMeta>> FIELD_CACHE = new WeakHashMap<>();

    /**
     * 字段元信息
     */
    public record FieldMeta(Field field, String loaderName) {}

    /**
     * 提取结果：loaderName -> Set<key>
     */
    public static Map<String, Set<Object>> extract(Object obj) {

        Map<String, Set<Object>> result  = new HashMap<>();
        Set<Object>              visited = Collections.newSetFromMap(new IdentityHashMap<>());
        doExtract(obj, result, visited);
        return result;
    }

    private static void doExtract(Object obj, Map<String, Set<Object>> result, Set<Object> visited) {

        if (obj == null) return;
        if (!visited.add(obj)) return;

        Class<?> clazz = obj.getClass();

        // 跳过基础类型
        if (isSkippedType(clazz)) return;

        // 处理集合
        if (obj instanceof Collection<?> collection) {
            for (Object item : collection) {
                doExtract(item, result, visited);
            }
            return;
        }

        // 处理 Map
        if (obj instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                doExtract(value, result, visited);
            }
            return;
        }

        // 处理数组
        if (clazz.isArray() && !clazz.getComponentType().isPrimitive()) {
            Object[] arr = (Object[]) obj;
            for (Object item : arr) {
                doExtract(item, result, visited);
            }
            return;
        }

        // 处理普通对象：扫描 @PopulateField 字段
        List<FieldMeta> populateFields = getPopulateFields(clazz);

        for (FieldMeta meta : populateFields) {
            Object value = getFieldValue(meta.field(), obj);
            if (value != null) {
                result.computeIfAbsent(meta.loaderName(), k -> new HashSet<>()).add(value);
            }
        }

        // 递归处理所有非基础类型字段
        ReflectionUtils.doWithFields(
                clazz,
                field -> {
                    if (isSkippedType(field.getType())) return;
                    if (field.isAnnotationPresent(PopulateField.class)) return;

                    Object value = getFieldValue(field, obj);
                    doExtract(value, result, visited);
                },
                field -> !java.lang.reflect.Modifier.isStatic(field.getModifiers())
        );
    }

    /**
     * 获取类中所有 @PopulateField 字段（带缓存）
     */
    private static List<FieldMeta> getPopulateFields(Class<?> clazz) {

        return FIELD_CACHE.computeIfAbsent(
                clazz,
                c -> {
                    List<FieldMeta> fields = new ArrayList<>();
                    ReflectionUtils.doWithFields(
                            c,
                            field -> {
                                PopulateField annotation = field.getAnnotation(PopulateField.class);
                                if (annotation != null) {
                                    ReflectionUtils.makeAccessible(field);
                                    fields.add(new FieldMeta(field, annotation.loader()));
                                }
                            }
                    );
                    return fields;
                }
        );
    }

    private static Object getFieldValue(Field field, Object obj) {

        try {
            ReflectionUtils.makeAccessible(field);
            return ReflectionUtils.getField(field, obj);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isSkippedType(Class<?> clazz) {

        return clazz.isPrimitive()
                || clazz.getName().startsWith("java.lang.")
                || clazz.getName().startsWith("java.time.")
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || clazz.isEnum();
    }

}