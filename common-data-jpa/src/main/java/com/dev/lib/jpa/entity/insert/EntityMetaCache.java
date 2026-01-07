package com.dev.lib.jpa.entity.insert;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EntityMetaCache {

    private static final Map<Class<?>, EntityMeta> CACHE = new ConcurrentHashMap<>();

    public static EntityMeta get(Class<?> entityClass) {

        return CACHE.computeIfAbsent(entityClass, EntityMetaCache::build);
    }

    private static EntityMeta build(Class<?> entityClass) {

        List<FieldMeta> fields  = new ArrayList<>();
        List<String>    columns = new ArrayList<>();
        for (Class<?> clazz = entityClass; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                if (shouldSkipField(field)) {  // 改这里
                    continue;
                }
                ReflectionUtils.makeAccessible(field);
                String columnName = resolveColumnName(field);

                // 这里调用
                FieldMeta meta = buildFieldMeta(field, columnName);

                fields.add(meta);
                columns.add(columnName);
            }
        }
        String tableName = resolveTableName(entityClass);
        String sql = "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") VALUES (" +
                columns.stream().map(c -> "?").collect(Collectors.joining(", ")) + ")";
        return new EntityMeta(sql, fields);
    }

    private static FieldMeta buildFieldMeta(Field field, String columnName) {

        int          sqlType      = resolveSqlType(field);
        boolean      isJson       = false;
        JdbcTypeCode jdbcTypeCode = field.getAnnotation(JdbcTypeCode.class);
        if (jdbcTypeCode != null && jdbcTypeCode.value() == SqlTypes.JSON) {
            isJson = true;
            sqlType = Types.OTHER; // PostgreSQL JSON
        }

        boolean isEnumString = false;
        if (field.getType().isEnum()) {
            Enumerated enumerated = field.getAnnotation(Enumerated.class);
            isEnumString = enumerated != null && enumerated.value() == EnumType.STRING;
            sqlType = isEnumString ? Types.VARCHAR : Types.INTEGER;
        }

        return new FieldMeta(field, columnName, sqlType, isJson, isEnumString);
    }

    private static boolean shouldSkipField(Field field) {

        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
            return true;
        }
        if (field.isAnnotationPresent(Transient.class)) {
            return true;
        }

        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null && !joinColumn.insertable()) {
            return true;
        }

        if (field.isAnnotationPresent(ManyToOne.class) || field.isAnnotationPresent(OneToOne.class)) {
            if (joinColumn == null) {
                return true;
            }
        }

        if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
            return true;
        }

        return false;
    }

    private static String resolveTableName(Class<?> entityClass) {

        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return camelToSnake(entityClass.getSimpleName());
    }

    private static String resolveColumnName(Field field) {

        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        return camelToSnake(field.getName());
    }

    private static int resolveSqlType(Field field) {

        Class<?> type = field.getType();
        if (type == Long.class || type == long.class) return Types.BIGINT;
        if (type == Integer.class || type == int.class) return Types.INTEGER;
        if (type == String.class) return Types.VARCHAR;
        if (type == Boolean.class || type == boolean.class) return Types.BOOLEAN;
        if (type == LocalDateTime.class) return Types.TIMESTAMP;
        if (type == BigDecimal.class) return Types.DECIMAL;
        if (Map.class.isAssignableFrom(type)) return Types.OTHER; // JSON
        return Types.OTHER;
    }

    private static String camelToSnake(String str) {

        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

}