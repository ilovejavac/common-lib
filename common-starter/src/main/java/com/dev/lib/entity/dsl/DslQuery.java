package com.dev.lib.entity.dsl;

import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class DslQuery<T> {
    private static final Map<Class<?>, EntityPathBase<?>> ENTITY_PATH_CACHE = new ConcurrentHashMap<>();

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final EntityPathBase<T> entityPath;

    @JsonIgnore
    protected LogicalOperator selfOperator = LogicalOperator.AND;

    // 新增: 嵌套子查询
    @JsonIgnore
    protected List<DslQuery<T>> orQueries = new ArrayList<>();

    @JsonIgnore
    protected List<DslQuery<T>> andQueries = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected DslQuery() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            Class<T> entityClass = (Class<T>) parameterizedType.getActualTypeArguments()[0];
            this.entityPath = (EntityPathBase<T>) ENTITY_PATH_CACHE.computeIfAbsent(
                    entityClass,
                    this::createEntityPath
            );
        } else {
            throw new IllegalStateException("必须指定泛型类型");
        }
    }

    @SuppressWarnings("unchecked")
    private EntityPathBase<T> createEntityPath(Class<?> entityClass) {
        try {
            String qClassName = entityClass.getPackage().getName() + ".Q" + entityClass.getSimpleName();
            Class<?> qClass = Class.forName(qClassName);

            String fieldName = Character.toLowerCase(entityClass.getSimpleName().charAt(0))
                    + entityClass.getSimpleName().substring(1);

            Field field = qClass.getDeclaredField(fieldName);
            ReflectionUtils.makeAccessible(field);

            return (EntityPathBase<T>) field.get(null);
        } catch (Exception e) {
            throw new IllegalStateException("无法创建 Q 类实例: " + entityClass.getName(), e);
        }
    }

    /**
     * 构建 Predicate
     */
    public static <T> BooleanBuilder toPredicate(DslQuery<?> q, BooleanExpression... expressions) {
        BooleanBuilder builder = new BooleanBuilder();

        if (q != null) {
            PathBuilder<?> pathBuilder = new PathBuilder<>(
                    q.entityPath.getType(),
                    q.entityPath.getMetadata()
            );

            Predicate result = processFieldsWithPrecedence(q, pathBuilder);
            if (result != null) {
                builder.and(result);
            }
        }

        for (BooleanExpression expression : expressions) {
            builder.and(expression);
        }

        return builder;
    }

    private static Predicate processFieldsWithPrecedence(
            DslQuery<?> q,
            PathBuilder<?> pathBuilder
    ) {
        List<Field> fields = Arrays.asList(q.getClass().getDeclaredFields());
        List<ExpressionItem> items = new ArrayList<>();

        // 按声明顺序收集表达式
        for (Field field : fields) {
            if (field.isAnnotationPresent(JsonIgnore.class)
                    || field.getName().equals("entityPath")
                    || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            ReflectionUtils.makeAccessible(field);
            Object value;
            try {
                value = field.get(q);
            } catch (IllegalAccessException e) {
                continue;
            }

            if (value == null) continue;

            // 处理嵌套 DslQuery 对象
            if (value instanceof DslQuery<?> nestedQuery) {
                Predicate nestedExpr = toPredicate(nestedQuery).getValue();
                if (nestedExpr != null) {
                    Condition condition = field.getAnnotation(Condition.class);
                    LogicalOperator operator = condition != null
                            ? condition.operator()
                            : LogicalOperator.AND;
                    items.add(new ExpressionItem(nestedExpr, operator));
                }
                continue;
            }

            // 处理普通字段
            Condition condition = field.getAnnotation(Condition.class);
            if (condition == null) continue;

            String targetField = condition.field().isEmpty()
                    ? field.getName()
                    : condition.field();

            BooleanExpression expr = q.buildExpression(
                    pathBuilder,
                    targetField,
                    condition.type(),
                    value
            );

            if (expr != null) {
                items.add(new ExpressionItem(expr, condition.operator()));
            }
        }

        if (items.isEmpty()) return null;

        // AND 优先级: 先处理所有 AND,再用 OR 连接
        return buildWithPrecedence(items);
    }

    // 新增: 按优先级构建表达式
    private static Predicate buildWithPrecedence(List<ExpressionItem> items) {
        if (items.isEmpty()) return null;
        if (items.size() == 1) return items.get(0).expression;

        // 分割成 AND 组 (遇到 OR 就分割)
        List<List<ExpressionItem>> andGroups = new ArrayList<>();
        List<ExpressionItem> currentGroup = new ArrayList<>();

        for (ExpressionItem item : items) {
            currentGroup.add(item);

            // 如果当前项标记为 OR,则分割
            if (item.operator == LogicalOperator.OR) {
                andGroups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
            }
        }

        // 最后一组
        if (!currentGroup.isEmpty()) {
            andGroups.add(currentGroup);
        }

        // 构建每个 AND 组
        List<Predicate> groupExpressions = new ArrayList<>();
        for (List<ExpressionItem> group : andGroups) {
            if (group.isEmpty()) continue;

            BooleanBuilder andBuilder = new BooleanBuilder();
            for (ExpressionItem item : group) {
                andBuilder.and(item.expression);
            }

            if (andBuilder.getValue() != null) {
                groupExpressions.add(andBuilder.getValue());
            }
        }

        // OR 连接所有 AND 组
        if (groupExpressions.isEmpty()) return null;
        if (groupExpressions.size() == 1) return groupExpressions.get(0);

        BooleanBuilder result = new BooleanBuilder();
        for (var expr : groupExpressions) {
            result.or(expr);
        }

        return result.getValue();
    }

    // 辅助类: 表达式项
    private static class ExpressionItem {
        Predicate expression;
        LogicalOperator operator;  // 此项与下一项的关系

        ExpressionItem(Predicate expression, LogicalOperator operator) {
            this.expression = expression;
            this.operator = operator;
        }
    }

    private BooleanExpression buildExpression(
            PathBuilder<?> pathBuilder,
            String field,
            QueryType type,
            Object value
    ) {

        // 处理嵌套字段：user.profile.name
        PathBuilder<?> currentPath = pathBuilder;
        String[] fieldParts = field.split("\\.");

        for (int i = 0; i < fieldParts.length - 1; i++) {
            currentPath = currentPath.get(fieldParts[i]);
        }

        String finalField = fieldParts[fieldParts.length - 1];
        return switch (type) {
            case EQ -> eq(currentPath, finalField, value);
            case NE -> ne(currentPath, finalField, value);
            case GT -> gt(currentPath, finalField, value);
            case GE -> goe(currentPath, finalField, value);
            case LT -> lt(currentPath, finalField, value);
            case LE -> loe(currentPath, finalField, value);
            case LIKE -> like(currentPath, finalField, value);
            case START_WITH -> startWith(currentPath, finalField, value);
            case END_WITH -> endWith(currentPath, finalField, value);
            case IN -> in(currentPath, finalField, value);
            case NOT_IN -> notIn(currentPath, finalField, value);
            case IS_NULL -> currentPath.get(finalField).isNull();
            case IS_NOT_NULL -> currentPath.get(finalField).isNotNull();
            default -> null;
        };
    }

// ========== 辅助方法 ==========

    private BooleanExpression eq(PathBuilder<?> path, String field, Object value) {
        return path.get(field).eq(value);
    }

    private BooleanExpression ne(PathBuilder<?> path, String field, Object value) {
        return path.get(field).ne(value);
    }

    @SuppressWarnings("unchecked")
    private BooleanExpression gt(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("GT 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .gt((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private BooleanExpression goe(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("GE 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .goe((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private BooleanExpression lt(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("LT 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .lt((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private BooleanExpression loe(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("LE 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .loe((Comparable<?>) value);
    }

    private BooleanExpression like(PathBuilder<?> path, String field, Object value) {
        return path.getString(field).containsIgnoreCase(value.toString());
    }

    private BooleanExpression startWith(PathBuilder<?> path, String field, Object value) {
        return path.getString(field).startsWithIgnoreCase(value.toString());
    }

    private BooleanExpression endWith(PathBuilder<?> path, String field, Object value) {
        return path.getString(field).endsWithIgnoreCase(value.toString());
    }


    private BooleanExpression in(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("IN 操作要求值必须是 Collection 类型");
        }
        if (CollectionUtils.isEmpty((Collection<?>) value)) {
            return null;
        }
        return path.get(field).in((Collection<?>) value);
    }

    private BooleanExpression notIn(PathBuilder<?> path, String field, Object value) {
        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("NOT_IN 操作要求值必须是 Collection 类型");
        }
        if (CollectionUtils.isEmpty((Collection<?>) value)) {
            return null;
        }

        return path.get(field).notIn((Collection<?>) value);
    }
}
