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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字段元数据缓存
 *
 * 支持三种字段类型：
 * 1. CONDITION - 普通条件/Join 条件
 * 2. GROUP - 条件分组
 * 3. SUB_QUERY - 子查询
 *
 * 判断逻辑：
 * 1. 后缀有 Sub → 子查询
 * 2. 注解 field 指向 JPA 关联 → 子查询
 * 3. 字段类型是自定义类且非关联 → 分组
 * 4. 其他 → 普通条件
 */
public class FieldMetaCache {

    private FieldMetaCache() {}

    private static final Map<Class<?>, ClassMeta> CACHE = new ConcurrentHashMap<>();

    public static ClassMeta getMeta(Class<?> queryClass) {
        return CACHE.computeIfAbsent(queryClass, FieldMetaCache::buildMeta);
    }

    private static ClassMeta buildMeta(Class<?> queryClass) {
        Class<?> entityClass = resolveEntityClass(queryClass);
        return new ClassMeta(entityClass, resolveFieldMeta(queryClass, entityClass));
    }

    public static List<FieldMeta> resolveFieldMeta(Class<?> queryClass) {
        Class<?> entityClass = null;
        try {
            entityClass = resolveEntityClass(queryClass);
        } catch (Exception ignored) {}
        return resolveFieldMeta(queryClass, entityClass);
    }

    public static List<FieldMeta> resolveFieldMeta(Class<?> queryClass, Class<?> entityClass) {
        List<FieldMeta> fieldMetas = new ArrayList<>();

        Class<?> current = queryClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (shouldSkip(field)) continue;

                ReflectionUtils.makeAccessible(field);
                FieldMeta meta = buildFieldMeta(field, entityClass);
                if (meta != null) {
                    fieldMetas.add(meta);
                }
            }
            current = current.getSuperclass();
        }

        return fieldMetas;
    }

    private static FieldMeta buildFieldMeta(Field field, Class<?> entityClass) {
        Condition condition = field.getAnnotation(Condition.class);
        QueryFieldParser.ParsedField parsed = QueryFieldParser.parse(field.getName());

        // 合并注解和后缀解析结果（注解优先）
        String targetField = resolveTargetField(condition, parsed);
        QueryType queryType = resolveQueryType(condition, parsed);
        LogicalOperator operator = resolveOperator(condition, parsed);
        boolean isSubQuery = resolveIsSubQuery(condition, parsed, field, targetField, entityClass);
        String select = condition != null ? condition.select() : "";
        String orderBy = condition != null ? condition.orderBy() : "";
        boolean desc = condition == null || condition.desc();

        // 判断字段类型
        FieldMetaType metaType = determineMetaType(field, isSubQuery);

        return switch (metaType) {
            case CONDITION -> FieldMeta.condition(field, targetField, queryType, operator);

            case GROUP -> {
                List<FieldMeta> nestedMetas = resolveFieldMeta(field.getType(), entityClass);
                yield FieldMeta.group(field, operator, nestedMetas);
            }

            case SUB_QUERY -> {
                // 通过接口解析关联信息
                RelationInfo relationInfo = resolveRelation(entityClass, targetField);
                if (relationInfo == null) {
                    throw new IllegalStateException(
                            "无法解析关联关系: " + entityClass.getSimpleName() + "." + targetField);
                }
                List<FieldMeta> filterMetas = resolveFieldMeta(field.getType(), relationInfo.getTargetEntity());
                yield FieldMeta.subQuery(field, operator, queryType, relationInfo, filterMetas, select, orderBy, desc);
            }
        };
    }

    /**
     * 通过 RelationResolver 接口解析关联关系
     */
    private static RelationInfo resolveRelation(Class<?> entityClass, String fieldName) {
        if (!RelationResolver.Holder.isRegistered()) {
            return null;
        }
        return RelationResolver.Holder.get().resolve(entityClass, fieldName);
    }

    /**
     * 判断是否为子查询
     */
    private static boolean resolveIsSubQuery(
            Condition condition,
            QueryFieldParser.ParsedField parsed,
            Field field,
            String targetField,
            Class<?> entityClass
    ) {
        // 1. 后缀有 Sub
        if (parsed.subQuery()) {
            return true;
        }

        // 2. 查询类型是 EXISTS / NOT_EXISTS
        QueryType type = resolveQueryType(condition, parsed);
        if (type == QueryType.EXISTS || type == QueryType.NOT_EXISTS) {
            return true;
        }

        // 3. 注解 field 指向 JPA 关联
        if (condition != null && StringUtils.hasText(condition.field()) && entityClass != null) {
            String fieldPath = condition.field();
            // 取第一段判断是否是关联字段
            String firstPart = fieldPath.contains(".") ? fieldPath.substring(0, fieldPath.indexOf(".")) : fieldPath;
            RelationInfo relation = resolveRelation(entityClass, firstPart);
            if (relation != null && !fieldPath.contains(".")) {
                // field 直接指向关联字段（非嵌套路径如 customer.name）
                return true;
            }
        }

        return false;
    }

    /**
     * 判断字段元数据类型
     */
    private static FieldMetaType determineMetaType(Field field, boolean isSubQuery) {
        // 1. 已确定是子查询
        if (isSubQuery) {
            return FieldMetaType.SUB_QUERY;
        }

        // 2. 基础类型 → 普通条件
        if (isSimpleType(field.getType())) {
            return FieldMetaType.CONDITION;
        }

        // 3. 自定义类 → 分组
        return FieldMetaType.GROUP;
    }

    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type.isEnum()
                || java.time.temporal.Temporal.class.isAssignableFrom(type)
                || java.util.Date.class.isAssignableFrom(type)
                || Collection.class.isAssignableFrom(type);
    }

    private static String resolveTargetField(Condition condition, QueryFieldParser.ParsedField parsed) {
        if (condition != null && StringUtils.hasText(condition.field())) {
            return condition.field();
        }
        return parsed.targetField();
    }

    private static QueryType resolveQueryType(Condition condition, QueryFieldParser.ParsedField parsed) {
        if (condition != null && condition.type() != QueryType.EMPTY) {
            return condition.type();
        }
        return parsed.queryType();
    }

    private static LogicalOperator resolveOperator(Condition condition, QueryFieldParser.ParsedField parsed) {
        // 后缀优先（更直观），注解可覆盖
        if (parsed.operator() == LogicalOperator.OR) {
            return LogicalOperator.OR;
        }
        if (condition != null) {
            return condition.operator();
        }
        return LogicalOperator.AND;
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

    // ═══════════════════════════════════════════════════════════════
    // 内部类型定义
    // ═══════════════════════════════════════════════════════════════

    public enum FieldMetaType {
        CONDITION,
        GROUP,
        SUB_QUERY
    }

    public record ClassMeta(Class<?> entityClass, List<FieldMeta> fields) {}

    /**
     * 字段元数据
     */
    public static class FieldMeta {
        private final Field field;
        private final FieldMetaType metaType;
        private final LogicalOperator operator;

        // CONDITION 字段
        private final String targetField;
        private final QueryType queryType;

        // GROUP 字段
        private final List<FieldMeta> nestedMetas;

        // SUB_QUERY 字段
        private final RelationInfo relationInfo;
        private final List<FieldMeta> filterMetas;
        private final String select;
        private final String orderBy;
        private final boolean desc;

        private FieldMeta(Field field, FieldMetaType metaType, LogicalOperator operator,
                          String targetField, QueryType queryType,
                          List<FieldMeta> nestedMetas,
                          RelationInfo relationInfo, List<FieldMeta> filterMetas,
                          String select, String orderBy, boolean desc) {
            this.field = field;
            this.metaType = metaType;
            this.operator = operator;
            this.targetField = targetField;
            this.queryType = queryType;
            this.nestedMetas = nestedMetas;
            this.relationInfo = relationInfo;
            this.filterMetas = filterMetas;
            this.select = select;
            this.orderBy = orderBy;
            this.desc = desc;
        }

        public static FieldMeta condition(Field field, String targetField,
                                          QueryType queryType, LogicalOperator operator) {
            return new FieldMeta(field, FieldMetaType.CONDITION, operator,
                    targetField, queryType, null, null, null, null, null, true);
        }

        public static FieldMeta group(Field field, LogicalOperator operator, List<FieldMeta> nestedMetas) {
            return new FieldMeta(field, FieldMetaType.GROUP, operator,
                    null, null, nestedMetas, null, null, null, null, true);
        }

        public static FieldMeta subQuery(Field field, LogicalOperator operator, QueryType queryType,
                                         RelationInfo relationInfo, List<FieldMeta> filterMetas,
                                         String select, String orderBy, boolean desc) {
            return new FieldMeta(field, FieldMetaType.SUB_QUERY, operator,
                    null, queryType, null, relationInfo, filterMetas, select, orderBy, desc);
        }

        public Object getValue(Object instance) {
            try {
                return ReflectionUtils.getField(field, instance);
            } catch (Exception e) {
                return null;
            }
        }

        // Getters
        public Field field() { return field; }
        public FieldMetaType metaType() { return metaType; }
        public LogicalOperator operator() { return operator; }
        public String targetField() { return targetField; }
        public QueryType queryType() { return queryType; }
        public List<FieldMeta> nestedMetas() { return nestedMetas; }
        public RelationInfo relationInfo() { return relationInfo; }
        public List<FieldMeta> filterMetas() { return filterMetas; }
        public String select() { return select; }
        public String orderBy() { return orderBy; }
        public boolean desc() { return desc; }

        public boolean isCondition() { return metaType == FieldMetaType.CONDITION; }
        public boolean isGroup() { return metaType == FieldMetaType.GROUP; }
        public boolean isSubQuery() { return metaType == FieldMetaType.SUB_QUERY; }
    }
}