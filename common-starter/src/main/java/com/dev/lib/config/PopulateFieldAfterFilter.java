package com.dev.lib.config;

import com.alibaba.fastjson2.filter.AfterFilter;
import com.dev.lib.web.serialize.PopulateContextHolder;
import com.dev.lib.web.serialize.PopulateField;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PopulateFieldAfterFilter extends AfterFilter {

    // 缓存类的 @PopulateField 字段信息
    private static final Map<Class<?>, FieldMeta[]> CACHE = new ConcurrentHashMap<>();

    @Override
    public void writeAfter(Object object) {

        if (object == null) return;

        FieldMeta[] metas = CACHE.computeIfAbsent(
                object.getClass(),
                this::parseFields
        );

        for (FieldMeta meta : metas) {
            Object idValue = getFieldValue(
                    object,
                    meta.field
            );
            Object populated = idValue != null
                               ? PopulateContextHolder.get(
                    meta.loaderName,
                    idValue
            )
                               : null;

            // 写入额外字段：{fieldName}{suffix}
            writeKeyValue(
                    meta.outputName,
                    populated
            );
        }
    }

    private FieldMeta[] parseFields(Class<?> clazz) {

        return java.util.Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(PopulateField.class))
                .map(f -> {
                    PopulateField ann = f.getAnnotation(PopulateField.class);
                    ReflectionUtils.makeAccessible(f);
                    return new FieldMeta(
                            f,
                            ann.loader(),
                            f.getName() + ann.suffix()
                    );
                })
                .toArray(FieldMeta[]::new);
    }

    private Object getFieldValue(Object obj, Field field) {

        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private record FieldMeta(Field field, String loaderName, String outputName) {}

}
