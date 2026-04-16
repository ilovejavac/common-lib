package com.dev.lib.jpa.entity.aggregate;

import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.agg.AggJoinStrategy;
import com.dev.lib.entity.dsl.agg.AggType;
import com.dev.lib.entity.dsl.agg.AggregateSpec;
import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.CollectionPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.springframework.util.ReflectionUtils;

import java.beans.ConstructorProperties;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AggregateExecutor<T extends JpaEntity> {

    private static final Map<Class<?>, Map<String, Field>> AGG_TARGET_FIELD_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, Map<String, Method>> AGG_TARGET_SETTER_CACHE = new ConcurrentHashMap<>(128);

    private static final Map<Class<?>, AggregateCtorPlan> AGG_TARGET_CTOR_PLAN_CACHE = new ConcurrentHashMap<>(128);

    private final EntityManager entityManager;

    private final EntityPath<T> path;

    private final PathBuilder<T> pathBuilder;

    public static <T extends JpaEntity, R> List<R> executeAggregateQuery(
            EntityManager entityManager,
            EntityPath<T> path,
            PathBuilder<T> pathBuilder,
            Predicate predicate,
            AggregateSpec<T, R> spec
    ) {

        return new AggregateExecutor<>(entityManager, path, pathBuilder).executeAggregateQuery(predicate, spec);
    }

    private AggregateExecutor(EntityManager entityManager, EntityPath<T> path, PathBuilder<T> pathBuilder) {

        this.entityManager = entityManager;
        this.path = path;
        this.pathBuilder = pathBuilder;
    }

    private <R> List<R> executeAggregateQuery(Predicate predicate, AggregateSpec<T, R> spec) {

        validateAggregateSpec(spec);
        JPAQuery<Tuple> query = new JPAQuery<>(entityManager);
        query.from(path);

        AggregateJoinContext joinContext = new AggregateJoinContext(query, spec.getJoinStrategy());
        Map<String, PathResolution> resolvedPathCache = new HashMap<>(spec.getItems().size() + spec.getGroupByFields().size());
        List<AliasedExpression> selectExpressions = new ArrayList<>(spec.getItems().size());
        Map<String, AggregateProjection> projectionByTarget = new LinkedHashMap<>(spec.getItems().size());
        for (AggregateSpec.Item item : spec.getItems()) {
            PathResolution source = resolveAggregatePath(item.sourceField(), joinContext, resolvedPathCache);
            Expression<?> projected = buildAggregateExpression(item.type(), source.expression());
            Expression<?> aliased = ExpressionUtils.as(projected, item.targetField());
            selectExpressions.add(new AliasedExpression(item.targetField(), aliased));
            projectionByTarget.put(item.targetField(), new AggregateProjection(projected));
        }
        query.select(selectExpressions.stream().map(AliasedExpression::aliasedExpression).toArray(Expression[]::new));

        if (predicate != null) {
            query.where(predicate);
        }

        if (!spec.getGroupByFields().isEmpty()) {
            Expression<?>[] groupByExpr = spec.getGroupByFields().stream()
                    .map(field -> resolveAggregatePath(field, joinContext, resolvedPathCache).expression())
                    .toArray(Expression[]::new);
            query.groupBy(groupByExpr);
        }

        applyAggregateHaving(query, spec, projectionByTarget);
        applyAggregateOrder(query, spec, projectionByTarget);
        applyAggregatePage(query, spec);

        List<Tuple> rows = query.fetch();
        return rows.stream().map(tuple -> mapAggregateRow(tuple, spec, selectExpressions)).toList();
    }

    private void validateAggregateSpec(AggregateSpec<T, ?> spec) {

        Set<String> groupedFields = new HashSet<>(spec.getGroupByFields());
        Map<String, AggregateSpec.Item> itemByTarget = new LinkedHashMap<>(spec.getItems().size());
        Map<String, Class<?>> sourceTypeCache = new HashMap<>();

        for (AggregateSpec.Item item : spec.getItems()) {
            AggregateSpec.Item duplicate = itemByTarget.putIfAbsent(item.targetField(), item);
            if (duplicate != null) {
                throw new IllegalArgumentException("聚合目标字段重复映射: " + item.targetField());
            }

            Class<?> sourceType = sourceTypeCache.computeIfAbsent(
                    item.sourceField(),
                    field -> resolveFieldType(path.getType(), field)
            );
            if ((item.type() == AggType.SUM || item.type() == AggType.AVG) && !Number.class.isAssignableFrom(boxed(sourceType))) {
                throw new IllegalArgumentException("聚合字段必须是数值类型: " + item.sourceField() + " for " + item.type());
            }
            if (item.type() == AggType.FIELD && !groupedFields.contains(item.sourceField())) {
                throw new IllegalArgumentException("FIELD 投影字段必须出现在 groupBy 中: " + item.sourceField());
            }
        }

        for (AggregateSpec.Having having : spec.getHavings()) {
            if (!itemByTarget.containsKey(having.targetField())) {
                throw new IllegalArgumentException("having 字段未映射: " + having.targetField());
            }
            if (having.queryType() != QueryType.IS_NULL
                    && having.queryType() != QueryType.IS_NOT_NULL
                    && having.value() == null) {
                throw new IllegalArgumentException("having 条件值不能为空: " + having.targetField());
            }
        }

        for (AggregateSpec.Order order : spec.getOrders()) {
            if (!itemByTarget.containsKey(order.targetField())) {
                throw new IllegalArgumentException("order 字段未映射: " + order.targetField());
            }
        }
    }

    private PathResolution resolveAggregatePath(String fieldPath, AggregateJoinContext joinContext, Map<String, PathResolution> resolvedPathCache) {

        return resolvedPathCache.computeIfAbsent(fieldPath, field -> resolveAggregatePath(field, joinContext));
    }

    private PathResolution resolveAggregatePath(String fieldPath, AggregateJoinContext joinContext) {

        String[] parts = fieldPath.split("\\.");
        if (parts.length == 0) {
            throw new IllegalArgumentException("无效字段路径: " + fieldPath);
        }

        if (joinContext.joinStrategy() == AggJoinStrategy.AUTO || parts.length == 1) {
            return resolvePathImplicit(fieldPath, parts);
        }
        return resolvePathWithJoin(fieldPath, parts, joinContext);
    }

    private PathResolution resolvePathImplicit(String fieldPath, String[] parts) {

        PathBuilder<?> current = pathBuilder;
        Class<?> currentType = path.getType();

        for (int i = 0; i < parts.length - 1; i++) {
            Field field = findField(currentType, parts[i]);
            if (field == null) {
                throw new IllegalArgumentException("字段不存在: " + fieldPath);
            }
            currentType = resolveFieldType(field);
            current = current.get(parts[i], currentType);
        }

        String last = parts[parts.length - 1];
        Field field = findField(currentType, last);
        if (field == null) {
            throw new IllegalArgumentException("字段不存在: " + fieldPath);
        }
        return new PathResolution(current.get(last));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private PathResolution resolvePathWithJoin(String fieldPath, String[] parts, AggregateJoinContext joinContext) {

        PathBuilder<?> currentPath = pathBuilder;
        Class<?> currentType = path.getType();
        StringBuilder relationPathBuilder = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Field field = findField(currentType, part);
            if (field == null) {
                throw new IllegalArgumentException("字段不存在: " + fieldPath);
            }
            Class<?> nextType = resolveFieldType(field);
            boolean relation = isJpaRelationField(field);

            if (relationPathBuilder.length() > 0) {
                relationPathBuilder.append('.');
            }
            relationPathBuilder.append(part);

            if (relation) {
                String joinKey = relationPathBuilder.toString();
                PathBuilder<?> alias = joinContext.joinedAliases().get(joinKey);
                if (alias == null) {
                    alias = new PathBuilder<>(nextType, "__agg_join_" + joinContext.joinedAliases().size());
                    if (Collection.class.isAssignableFrom(field.getType())) {
                        CollectionPath collectionPath = currentPath.getCollection(part, nextType);
                        if (joinContext.joinStrategy() == AggJoinStrategy.LEFT) {
                            joinContext.query().leftJoin(collectionPath, alias);
                        } else {
                            joinContext.query().innerJoin(collectionPath, alias);
                        }
                    } else {
                        PathBuilder<?> relationPath = currentPath.get(part, nextType);
                        if (joinContext.joinStrategy() == AggJoinStrategy.LEFT) {
                            joinContext.query().leftJoin((EntityPath) relationPath, (Path) alias);
                        } else {
                            joinContext.query().innerJoin((EntityPath) relationPath, (Path) alias);
                        }
                    }
                    joinContext.joinedAliases().put(joinKey, alias);
                }
                currentPath = alias;
            } else {
                currentPath = currentPath.get(part, nextType);
            }

            currentType = nextType;
        }

        String last = parts[parts.length - 1];
        Field field = findField(currentType, last);
        if (field == null) {
            throw new IllegalArgumentException("字段不存在: " + fieldPath);
        }
        return new PathResolution(currentPath.get(last));
    }

    private static Expression<?> buildAggregateExpression(AggType type, Expression<?> source) {

        return switch (type) {
            case FIELD -> source;
            case COUNT -> Expressions.numberTemplate(Long.class, "count({0})", source);
            case COUNT_DISTINCT -> Expressions.numberTemplate(Long.class, "count(distinct {0})", source);
            case SUM -> Expressions.numberTemplate(BigDecimal.class, "sum({0})", source);
            case MIN -> Expressions.comparableTemplate(Comparable.class, "min({0})", source);
            case MAX -> Expressions.comparableTemplate(Comparable.class, "max({0})", source);
            case AVG -> Expressions.numberTemplate(BigDecimal.class, "avg({0})", source);
        };
    }

    private static void applyAggregateHaving(
            JPAQuery<Tuple> query,
            AggregateSpec<?, ?> spec,
            Map<String, AggregateProjection> projectionByTarget
    ) {

        if (spec.getHavings().isEmpty()) {
            return;
        }

        BooleanBuilder havingBuilder = new BooleanBuilder();
        for (AggregateSpec.Having having : spec.getHavings()) {
            AggregateProjection projection = requireAggregateProjection(projectionByTarget, having.targetField(), "having");
            Predicate predicate = buildHavingPredicate(
                    projection.expression(),
                    having.queryType(),
                    having.value()
            );
            if (predicate != null) {
                havingBuilder.and(predicate);
            }
        }

        if (havingBuilder.getValue() != null) {
            query.having(havingBuilder.getValue());
        }
    }

    private static Predicate buildHavingPredicate(
            Expression<?> expression,
            QueryType queryType,
            Object value
    ) {

        if (queryType == null || queryType == QueryType.EMPTY) {
            throw new IllegalArgumentException("having 查询类型不能为空");
        }

        return switch (queryType) {
            case EQ -> Expressions.booleanTemplate("{0} = {1}", expression, Expressions.constant(value));
            case NE -> Expressions.booleanTemplate("{0} <> {1}", expression, Expressions.constant(value));
            case GT -> Expressions.booleanTemplate("{0} > {1}", expression, Expressions.constant(value));
            case GE -> Expressions.booleanTemplate("{0} >= {1}", expression, Expressions.constant(value));
            case LT -> Expressions.booleanTemplate("{0} < {1}", expression, Expressions.constant(value));
            case LE -> Expressions.booleanTemplate("{0} <= {1}", expression, Expressions.constant(value));
            case LIKE -> Expressions.stringTemplate("str({0})", expression).containsIgnoreCase(String.valueOf(value));
            case START_WITH -> Expressions.stringTemplate("str({0})", expression).startsWithIgnoreCase(String.valueOf(value));
            case END_WITH -> Expressions.stringTemplate("str({0})", expression).endsWithIgnoreCase(String.valueOf(value));
            case IN -> {
                if (!(value instanceof Collection<?> values) || values.isEmpty()) {
                    yield Expressions.booleanTemplate("1=0");
                }
                yield Expressions.booleanTemplate("{0} in {1}", expression, Expressions.constant(values));
            }
            case NOT_IN -> {
                if (!(value instanceof Collection<?> values) || values.isEmpty()) {
                    yield Expressions.booleanTemplate("1=1");
                }
                yield Expressions.booleanTemplate("{0} not in {1}", expression, Expressions.constant(values));
            }
            case IS_NULL -> Expressions.booleanTemplate("{0} is null", expression);
            case IS_NOT_NULL -> Expressions.booleanTemplate("{0} is not null", expression);
            default -> throw new IllegalArgumentException("having 不支持查询类型: " + queryType);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void applyAggregateOrder(
            JPAQuery<Tuple> query,
            AggregateSpec<?, ?> spec,
            Map<String, AggregateProjection> projectionByTarget
    ) {

        if (spec.getOrders().isEmpty()) {
            return;
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>(spec.getOrders().size());
        for (AggregateSpec.Order order : spec.getOrders()) {
            AggregateProjection projection = requireAggregateProjection(projectionByTarget, order.targetField(), "order");
            Expression<Comparable> orderExpression = Expressions.comparableTemplate(
                    Comparable.class,
                    "{0}",
                    projection.expression()
            );
            orders.add(new OrderSpecifier(order.asc() ? Order.ASC : Order.DESC, orderExpression));
        }
        query.orderBy(orders.toArray(OrderSpecifier[]::new));
    }

    private static AggregateProjection requireAggregateProjection(
            Map<String, AggregateProjection> projectionByTarget,
            String targetField,
            String stage
    ) {

        AggregateProjection projection = projectionByTarget.get(targetField);
        if (projection == null) {
            throw new IllegalArgumentException(stage + " 字段未映射: " + targetField);
        }
        return projection;
    }

    private static void applyAggregatePage(JPAQuery<Tuple> query, AggregateSpec<?, ?> spec) {

        if (spec.getOffset() != null) {
            query.offset(spec.getOffset());
        }
        if (spec.getLimit() != null) {
            query.limit(spec.getLimit());
        }
    }

    private static <R> R mapAggregateRow(
            Tuple tuple,
            AggregateSpec<?, R> spec,
            List<AliasedExpression> selectExpressions
    ) {

        Map<String, Object> valueByField = new HashMap<>(selectExpressions.size());
        for (int i = 0; i < selectExpressions.size(); i++) {
            AliasedExpression aliasedExpression = selectExpressions.get(i);
            Object value = tuple.get(i, Object.class);
            valueByField.put(aliasedExpression.targetField(), value);
        }

        AggregateCtorPlan ctorPlan = getAggregateCtorPlan(spec.getTargetClass());
        return switch (ctorPlan.type()) {
            case NO_ARGS -> mapByNoArgsCtor(spec.getTargetClass(), valueByField, ctorPlan.constructor());
            case ALL_ARGS -> mapByArgsCtor(spec.getTargetClass(), valueByField, ctorPlan);
        };
    }

    @SuppressWarnings("unchecked")
    private static <R> R mapByNoArgsCtor(Class<R> targetClass, Map<String, Object> valueByField, Constructor<?> constructor) {

        R instance;
        try {
            instance = (R) constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("聚合结果对象构造失败: " + targetClass.getName(), e);
        }

        Map<String, Method> setters = getAggTargetSetters(targetClass);
        Map<String, Field> fields = getAggTargetFields(targetClass);
        for (Map.Entry<String, Object> entry : valueByField.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            Method setter = setters.get(fieldName);
            if (setter != null) {
                ReflectionUtils.invokeMethod(
                        setter,
                        instance,
                        convertAggregateValue(value, setter.getParameterTypes()[0])
                );
                continue;
            }

            Field field = fields.get(fieldName);
            if (field != null) {
                ReflectionUtils.setField(field, instance, convertAggregateValue(value, field.getType()));
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static <R> R mapByArgsCtor(Class<R> targetClass, Map<String, Object> valueByField, AggregateCtorPlan plan) {

        try {
            Constructor<?> constructor = plan.constructor();
            String[] names = plan.argNames();
            Class<?>[] argTypes = constructor.getParameterTypes();
            Object[] args = new Object[argTypes.length];
            for (int i = 0; i < argTypes.length; i++) {
                args[i] = convertAggregateValue(valueByField.get(names[i]), argTypes[i]);
            }
            return (R) constructor.newInstance(args);
        } catch (Exception e) {
            throw new IllegalStateException("聚合结果对象构造失败: " + targetClass.getName(), e);
        }
    }

    private static AggregateCtorPlan getAggregateCtorPlan(Class<?> targetClass) {

        return AGG_TARGET_CTOR_PLAN_CACHE.computeIfAbsent(targetClass, AggregateExecutor::buildAggregateCtorPlan);
    }

    private static AggregateCtorPlan buildAggregateCtorPlan(Class<?> targetClass) {

        try {
            Constructor<?> constructor = targetClass.getDeclaredConstructor();
            ReflectionUtils.makeAccessible(constructor);
            return new AggregateCtorPlan(AggregateCtorPlanType.NO_ARGS, constructor, new String[0]);
        } catch (NoSuchMethodException ignored) {
            // fall through to other strategies
        }

        if (targetClass.isRecord()) {
            return buildRecordCtorPlan(targetClass);
        }

        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            ConstructorProperties properties = constructor.getAnnotation(ConstructorProperties.class);
            if (properties != null) {
                String[] names = properties.value();
                if (names.length == constructor.getParameterCount()) {
                    ReflectionUtils.makeAccessible(constructor);
                    return new AggregateCtorPlan(AggregateCtorPlanType.ALL_ARGS, constructor, names);
                }
            }
        }

        if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
            Constructor<?> constructor = constructors[0];
            String[] names = Arrays.stream(constructor.getParameters()).map(Parameter::getName).toArray(String[]::new);
            boolean hasRealNames = Arrays.stream(names).noneMatch(name -> name.startsWith("arg"));
            if (hasRealNames) {
                ReflectionUtils.makeAccessible(constructor);
                return new AggregateCtorPlan(AggregateCtorPlanType.ALL_ARGS, constructor, names);
            }
        }

        throw new IllegalStateException("聚合结果类型必须有无参构造、Record 构造或 @ConstructorProperties 构造: " + targetClass.getName());
    }

    private static AggregateCtorPlan buildRecordCtorPlan(Class<?> targetClass) {

        try {
            RecordComponent[] components = targetClass.getRecordComponents();
            Class<?>[] argTypes = Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);
            String[] argNames = Arrays.stream(components).map(RecordComponent::getName).toArray(String[]::new);
            Constructor<?> constructor = targetClass.getDeclaredConstructor(argTypes);
            ReflectionUtils.makeAccessible(constructor);
            return new AggregateCtorPlan(AggregateCtorPlanType.ALL_ARGS, constructor, argNames);
        } catch (Exception e) {
            throw new IllegalStateException("Record 聚合结果构造解析失败: " + targetClass.getName(), e);
        }
    }

    private static Map<String, Method> getAggTargetSetters(Class<?> targetClass) {

        return AGG_TARGET_SETTER_CACHE.computeIfAbsent(targetClass, clazz -> {
            Map<String, Method> setters = new HashMap<>();
            for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getParameterCount() != 1 || !method.getName().startsWith("set") || method.getName().length() <= 3) {
                        continue;
                    }
                    String name = Character.toLowerCase(method.getName().charAt(3)) + method.getName().substring(4);
                    ReflectionUtils.makeAccessible(method);
                    setters.putIfAbsent(name, method);
                }
            }
            return setters;
        });
    }

    private static Object convertAggregateValue(Object value, Class<?> rawTargetType) {

        if (value == null) {
            return null;
        }
        Class<?> targetType = boxed(rawTargetType);
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == Long.class && value instanceof Number number) {
            return number.longValue();
        }
        if (targetType == Integer.class && value instanceof Number number) {
            return number.intValue();
        }
        if (targetType == Double.class && value instanceof Number number) {
            return number.doubleValue();
        }
        if (targetType == Float.class && value instanceof Number number) {
            return number.floatValue();
        }
        if (targetType == Short.class && value instanceof Number number) {
            return number.shortValue();
        }
        if (targetType == Byte.class && value instanceof Number number) {
            return number.byteValue();
        }
        if (targetType == BigDecimal.class) {
            if (value instanceof BigDecimal) {
                return value;
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return value;
    }

    private static Map<String, Field> getAggTargetFields(Class<?> targetClass) {

        return AGG_TARGET_FIELD_CACHE.computeIfAbsent(targetClass, clazz -> {
            Map<String, Field> fields = new HashMap<>();
            for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    ReflectionUtils.makeAccessible(field);
                    fields.putIfAbsent(field.getName(), field);
                }
            }
            return fields;
        });
    }

    private static Field findField(Class<?> type, String name) {

        Field field = ReflectionUtils.findField(type, name);
        if (field != null) {
            ReflectionUtils.makeAccessible(field);
        }
        return field;
    }

    private static Class<?> resolveFieldType(Class<?> rootType, String fieldPath) {

        String[] parts = fieldPath.split("\\.");
        Class<?> currentType = rootType;
        for (String part : parts) {
            Field field = findField(currentType, part);
            if (field == null) {
                throw new IllegalArgumentException("字段不存在: " + fieldPath);
            }
            currentType = resolveFieldType(field);
        }
        return currentType;
    }

    private static Class<?> resolveFieldType(Field field) {

        if (!Collection.class.isAssignableFrom(field.getType())) {
            return boxed(field.getType());
        }

        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class<?> argType) {
                return boxed(argType);
            }
        }
        return Object.class;
    }

    private static boolean isJpaRelationField(Field field) {

        return field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToOne.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    private static Class<?> boxed(Class<?> type) {

        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private record AliasedExpression(String targetField, Expression<?> aliasedExpression) {
    }

    private record AggregateProjection(Expression<?> expression) {
    }

    private record PathResolution(Expression<?> expression) {
    }

    private record AggregateJoinContext(
            JPAQuery<Tuple> query,
            AggJoinStrategy joinStrategy,
            Map<String, PathBuilder<?>> joinedAliases
    ) {

        AggregateJoinContext(JPAQuery<Tuple> query, AggJoinStrategy joinStrategy) {

            this(query, joinStrategy, new LinkedHashMap<>());
        }
    }

    private enum AggregateCtorPlanType {
        NO_ARGS,
        ALL_ARGS
    }

    private record AggregateCtorPlan(AggregateCtorPlanType type, Constructor<?> constructor, String[] argNames) {
    }
}
