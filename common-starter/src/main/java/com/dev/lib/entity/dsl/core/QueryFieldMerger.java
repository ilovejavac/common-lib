package com.dev.lib.entity.dsl.core;

import com.dev.lib.entity.BaseEntity;
import com.dev.lib.entity.dsl.Condition;
import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class QueryFieldMerger {
    private QueryFieldMerger() {
    }

    public static <E extends BaseEntity> Map<String, ExternalFieldInfo> merge(
            DslQuery<E> target,
            Object query
    ) {
        Map<String, ExternalFieldInfo> externalFields = new HashMap<>();
        if (query == null) return externalFields;

        Map<String, Field> targetFieldMap = buildFieldMap(target.getClass());

        for (Field sourceField : query.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(sourceField.getModifiers())) continue;

            ReflectionUtils.makeAccessible(sourceField);
            Object value = getFieldValue(sourceField, query);

            if (value == null) continue;

            Condition condition = sourceField.getAnnotation(Condition.class);
            if (condition == null) continue;

            // 尝试赋值给目标字段
            if (!tryAssignToTarget(sourceField, value, targetFieldMap, target)) {
                // 收集为外部字段
                String targetField = condition.field().isEmpty()
                        ? sourceField.getName()
                        : condition.field();

                externalFields.put(
                        sourceField.getName(), new ExternalFieldInfo(
                                targetField,
                                condition.type(),
                                condition.operator(),
                                value
                        )
                );
            }
        }

        return externalFields;
    }

    private static Map<String, Field> buildFieldMap(Class<?> clazz) {
        Map<String, Field> map = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                map.put(field.getName(), field);
            }
        }
        return map;
    }

    private static Object getFieldValue(Field field, Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static <E extends BaseEntity> boolean tryAssignToTarget(
            Field sourceField,
            Object value,
            Map<String, Field> targetFieldMap,
            DslQuery<E> target
    ) {
        Field targetField = targetFieldMap.get(sourceField.getName());
        if (targetField == null || Modifier.isFinal(targetField.getModifiers())) {
            return false;
        }

        ReflectionUtils.makeAccessible(targetField);
        try {
            ReflectionUtils.setField(targetField, target, value);
            return true;
        } catch (Exception e) {
            log.warn("字段赋值失败 {}: {}", sourceField.getName(), e.getMessage());
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    public static class ExternalFieldInfo {
        String targetField;
        QueryType type;
        LogicalOperator operator;
        Object value;
    }
}