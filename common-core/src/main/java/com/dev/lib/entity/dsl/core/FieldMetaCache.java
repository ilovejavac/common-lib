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
 * <p>
 * 支持三种字段类型：
 * 1. CONDITION - 普通条件/Join 条件
 * 2. GROUP - 条件分组
 * 3. SUB_QUERY - 子查询（包括关联子查询和同表子查询）
 * <p>
 * 判断逻辑：
 * 1. 后缀有 Sub → 子查询
 * 2. 注解 field 指向 JPA 关联 → 关联子查询
 * 3. 后缀有 Sub 但 field 非关联 → 同表子查询
 * 4. 字段类型是自定义类且非关联 → 分组
 * 5. 其他 → 普通条件
 */
public class FieldMetaCache {

    private FieldMetaCache() {

    }

    private static final Map<Class<?>, ClassMeta> CACHE = new ConcurrentHashMap<>(512);

    public static ClassMeta getMeta(Class<?> queryClass) {

        return CACHE.computeIfAbsent(
                queryClass,
                FieldMetaCache::buildMeta
        );
    }

    private static ClassMeta buildMeta(Class<?> queryClass) {

        Class<?> entityClass = resolveEntityClass(queryClass);
        return new ClassMeta(
                entityClass,
                resolveFieldMeta(
                        queryClass,
                        entityClass
                )
        );
    }

    public static List<FieldMeta> resolveFieldMeta(Class<?> queryClass) {

        Class<?> entityClass = null;
        try {
            entityClass = resolveEntityClass(queryClass);
        } catch (Exception ignored) {
        }
        return resolveFieldMeta(
                queryClass,
                entityClass
        );
    }

    public static List<FieldMeta> resolveFieldMeta(Class<?> queryClass, Class<?> entityClass) {

        List<FieldMeta> fieldMetas = new ArrayList<>();

        Class<?> current = queryClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (shouldSkip(field)) continue;

                ReflectionUtils.makeAccessible(field);
                FieldMeta meta = buildFieldMeta(
                        field,
                        entityClass
                );
                if (meta != null) {
                    fieldMetas.add(meta);
                }
            }
            current = current.getSuperclass();
        }

        return fieldMetas;
    }

    private static FieldMeta buildFieldMeta(Field field, Class<?> entityClass) {

        Condition                    condition = field.getAnnotation(Condition.class);
        QueryFieldParser.ParsedField parsed    = QueryFieldParser.parse(field.getName());

        // 合并注解和后缀解析结果（注解优先）
        String targetField = resolveTargetField(
                condition,
                parsed
        );
        QueryType queryType = resolveQueryType(
                condition,
                parsed
        );
        LogicalOperator operator = resolveOperator(
                condition,
                parsed
        );
        String  select  = condition != null ? condition.select() : "";
        String  orderBy = condition != null ? condition.orderBy() : "";
        boolean desc    = condition == null || condition.desc();

        // 判断字段元数据类型
        FieldMetaType metaType = determineMetaType(
                field,
                parsed,
                condition,
                targetField,
                entityClass
        );

        return switch (metaType) {
            case CONDITION -> FieldMeta.condition(
                    field,
                    targetField,
                    queryType,
                    operator
            );

            case GROUP -> {
                List<FieldMeta> nestedMetas = resolveFieldMeta(
                        field.getType(),
                        entityClass
                );
                yield FieldMeta.group(
                        field,
                        operator,
                        nestedMetas
                );
            }

            case SUB_QUERY -> {
                RelationInfo relationInfo = resolveRelation(
                        entityClass,
                        targetField
                );
                if (relationInfo != null) {
                    // 关联子查询（JPA）
                    List<FieldMeta> filterMetas = resolveFieldMeta(
                            field.getType(),
                            relationInfo.getTargetEntity()
                    );
                    yield FieldMeta.subQuery(
                            field,
                            operator,
                            queryType,
                            relationInfo,
                            filterMetas,
                            select,
                            orderBy,
                            desc
                    );
                } else {
                    // 同表子查询（JPA）或嵌套文档查询（MongoDB）
                    // 如果没有 select，尝试从字段名推断（供 MongoDB 使用）
                    String inferredPath = null;
                    if (!StringUtils.hasText(select)) {
                        String fieldName = field.getName();
                        if (fieldName.endsWith("ExistsSub")) {
                            inferredPath = fieldName.substring(
                                    0,
                                    fieldName.length() - 9
                            );
                        } else if (fieldName.endsWith("NotExistsSub")) {
                            inferredPath = fieldName.substring(
                                    0,
                                    fieldName.length() - 12
                            );
                        } else if (fieldName.endsWith("Sub")) {
                            inferredPath = fieldName.substring(
                                    0,
                                    fieldName.length() - 3
                            );
                        }
                    }

                    // 使用 select 或推断路径作为 parentField
                    String parentField = StringUtils.hasText(select) ? select : inferredPath;

                    // JPA 必须有 select，MongoDB 可以推断
                    if (parentField == null) {
                        throw new IllegalStateException(
                                "同表子查询必须指定 @Condition(select = \"...\") 属性或使用 Sub 后缀: " +
                                        (entityClass != null
                                         ? entityClass.getSimpleName()
                                         : "Unknown") + "." + field.getName());
                    }

                    List<FieldMeta> filterMetas = resolveFieldMeta(
                            field.getType(),
                            entityClass
                    );
                    yield FieldMeta.selfSubQuery(
                            field,
                            operator,
                            queryType,
                            entityClass,
                            filterMetas,
                            select,
                            orderBy,
                            desc,
                            parentField
                    );
                }
            }
        };
    }

    /**
     * 通过 RelationResolver 接口解析关联关系
     */
    private static RelationInfo resolveRelation(Class<?> entityClass, String fieldName) {

        if (entityClass == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        if (!RelationResolver.Holder.isRegistered()) {
            return null;
        }
        return RelationResolver.Holder.get().resolve(
                entityClass,
                fieldName
        );
    }

    /**
     * 判断字段元数据类型
     */
    private static FieldMetaType determineMetaType(
            Field field,
            QueryFieldParser.ParsedField parsed,
            Condition condition,
            String targetField,
            Class<?> entityClass
    ) {
        // 1. 后缀有 Sub → 子查询
        if (parsed.subQuery()) {
            return FieldMetaType.SUB_QUERY;
        }

        // 2. 查询类型是 EXISTS / NOT_EXISTS → 子查询
        QueryType type = resolveQueryType(
                condition,
                parsed
        );
        if (type == QueryType.EXISTS || type == QueryType.NOT_EXISTS) {
            return FieldMetaType.SUB_QUERY;
        }

        // 3. 注解 field 直接指向 JPA 关联字段 → 子查询
        if (condition != null && StringUtils.hasText(condition.field()) && entityClass != null) {
            String fieldPath = condition.field();
            String firstPart = fieldPath.contains(".") ? fieldPath.substring(
                    0,
                    fieldPath.indexOf(".")
            ) : fieldPath;
            RelationInfo relation = resolveRelation(
                    entityClass,
                    firstPart
            );
            if (relation != null && !fieldPath.contains(".")) {
                return FieldMetaType.SUB_QUERY;
            }
        }

        // 4. 基础类型 → 普通条件
        if (isSimpleType(field.getType())) {
            return FieldMetaType.CONDITION;
        }

        // 5. 自定义类 → 分组
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

        // SUB_QUERY 字段（关联子查询）
        private final RelationInfo relationInfo;

        private final List<FieldMeta> filterMetas;

        private final String select;

        private final String orderBy;

        private final boolean desc;

        // SUB_QUERY 字段（同表子查询）
        private final Class<?> targetEntityClass;

        private final String parentField;

        private FieldMeta(Field field, FieldMetaType metaType, LogicalOperator operator,
                          String targetField, QueryType queryType,
                          List<FieldMeta> nestedMetas,
                          RelationInfo relationInfo, List<FieldMeta> filterMetas,
                          String select, String orderBy, boolean desc,
                          Class<?> targetEntityClass, String parentField) {

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
            this.targetEntityClass = targetEntityClass;
            this.parentField = parentField;
        }

        public static FieldMeta condition(Field field, String targetField,
                                          QueryType queryType, LogicalOperator operator) {

            return new FieldMeta(
                    field,
                    FieldMetaType.CONDITION,
                    operator,
                    targetField,
                    queryType,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null
            );
        }

        public static FieldMeta group(Field field, LogicalOperator operator, List<FieldMeta> nestedMetas) {

            return new FieldMeta(
                    field,
                    FieldMetaType.GROUP,
                    operator,
                    null,
                    null,
                    nestedMetas,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null
            );
        }

        /**
         * 关联子查询（OneToMany, ManyToOne, ManyToMany, OneToOne）
         */
        public static FieldMeta subQuery(Field field, LogicalOperator operator, QueryType queryType,
                                         RelationInfo relationInfo, List<FieldMeta> filterMetas,
                                         String select, String orderBy, boolean desc) {

            return new FieldMeta(
                    field,
                    FieldMetaType.SUB_QUERY,
                    operator,
                    null,
                    queryType,
                    null,
                    relationInfo,
                    filterMetas,
                    select,
                    orderBy,
                    desc,
                    null,
                    null
            );
        }

        /**
         * 同表子查询（普通字段，如游标分页）
         *
         * @param targetEntityClass 目标实体类（同表）
         * @param filterMetas       子查询过滤条件
         * @param select            子查询 SELECT 字段
         * @param parentField       主表比较字段
         */
        public static FieldMeta selfSubQuery(Field field, LogicalOperator operator, QueryType queryType,
                                             Class<?> targetEntityClass, List<FieldMeta> filterMetas,
                                             String select, String orderBy, boolean desc, String parentField) {

            return new FieldMeta(
                    field,
                    FieldMetaType.SUB_QUERY,
                    operator,
                    null,
                    queryType,
                    null,
                    null,
                    filterMetas,
                    select,
                    orderBy,
                    desc,
                    targetEntityClass,
                    parentField
            );
        }

        public Object getValue(Object instance) {

            try {
                return ReflectionUtils.getField(
                        field,
                        instance
                );
            } catch (Exception e) {
                return null;
            }
        }

        // Getters
        public Field field() {

            return field;
        }

        public FieldMetaType metaType() {

            return metaType;
        }

        public LogicalOperator operator() {

            return operator;
        }

        public String targetField() {

            return targetField;
        }

        public QueryType queryType() {

            return queryType;
        }

        public List<FieldMeta> nestedMetas() {

            return nestedMetas;
        }

        public RelationInfo relationInfo() {

            return relationInfo;
        }

        public List<FieldMeta> filterMetas() {

            return filterMetas;
        }

        public String select() {

            return select;
        }

        public String orderBy() {

            return orderBy;
        }

        public boolean desc() {

            return desc;
        }

        public Class<?> targetEntityClass() {

            return targetEntityClass;
        }

        public String parentField() {

            return parentField;
        }

        public boolean isCondition() {

            return metaType == FieldMetaType.CONDITION;
        }

        public boolean isGroup() {

            return metaType == FieldMetaType.GROUP;
        }

        public boolean isSubQuery() {

            return metaType == FieldMetaType.SUB_QUERY;
        }

        /**
         * 判断是否为同表子查询
         */
        public boolean isSelfSubQuery() {

            return isSubQuery() && relationInfo == null && targetEntityClass != null;
        }

        /**
         * 判断是否为关联子查询
         */
        public boolean isRelationSubQuery() {

            return isSubQuery() && relationInfo != null;
        }

    }

}