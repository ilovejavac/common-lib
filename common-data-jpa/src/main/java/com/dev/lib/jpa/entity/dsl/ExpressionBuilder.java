package com.dev.lib.jpa.entity.dsl;

import com.dev.lib.entity.dsl.QueryType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExpressionBuilder {

    private ExpressionBuilder() {

    }

    public static BooleanExpression build(
            PathBuilder<?> pathBuilder,
            String field,
            QueryType type,
            Object value
    ) {

        if (value == null && type != QueryType.IS_NULL && type != QueryType.IS_NOT_NULL) {
            return null;
        }

        String[] fieldParts = field.split("\\.");
        return build(pathBuilder, fieldParts, type, value);
    }

    public static BooleanExpression build(
            PathBuilder<?> pathBuilder,
            String[] fieldParts,
            QueryType type,
            Object value
    ) {

        if (value == null && type != QueryType.IS_NULL && type != QueryType.IS_NOT_NULL) {
            return null;
        }

        PathBuilder<?> currentPath = resolveParentPath(pathBuilder, fieldParts);

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
            case BETWEEN -> between(currentPath, finalField, value);
            default -> null;
        };
    }

    static BooleanExpression between(
            PathBuilder<?> pathBuilder,
            String[] fieldParts,
            Object lowerBound,
            Object upperBound
    ) {

        PathBuilder<?> currentPath = resolveParentPath(pathBuilder, fieldParts);
        String finalField = fieldParts[fieldParts.length - 1];
        return between(currentPath, finalField, lowerBound, upperBound);
    }

    private static PathBuilder<?> resolveParentPath(PathBuilder<?> pathBuilder, String[] fieldParts) {

        PathBuilder<?> currentPath = pathBuilder;
        for (int i = 0; i < fieldParts.length - 1; i++) {
            currentPath = currentPath.get(fieldParts[i]);
        }
        return currentPath;
    }

    private static BooleanExpression eq(PathBuilder<?> path, String field, Object value) {

        return path.get(field).eq(value);
    }

    private static BooleanExpression ne(PathBuilder<?> path, String field, Object value) {

        return path.get(field).ne(value);
    }

    @SuppressWarnings("unchecked")
    private static BooleanExpression gt(PathBuilder<?> path, String field, Object value) {

        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("GT 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .gt((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private static BooleanExpression goe(PathBuilder<?> path, String field, Object value) {

        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("GE 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .goe((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private static BooleanExpression lt(PathBuilder<?> path, String field, Object value) {

        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("LT 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .lt((Comparable<?>) value);
    }

    @SuppressWarnings("unchecked")
    private static BooleanExpression loe(PathBuilder<?> path, String field, Object value) {

        if (!(value instanceof Comparable)) {
            throw new IllegalArgumentException("LE 操作要求值必须实现 Comparable 接口");
        }
        return path.getComparable(field, (Class<Comparable>) value.getClass())
                .loe((Comparable<?>) value);
    }

    private static BooleanExpression like(PathBuilder<?> path, String field, Object value) {

        return path.getString(field).containsIgnoreCase(value.toString());
    }

    private static BooleanExpression startWith(PathBuilder<?> path, String field, Object value) {

        return path.getString(field).startsWithIgnoreCase(value.toString());
    }

    private static BooleanExpression endWith(PathBuilder<?> path, String field, Object value) {

        return path.getString(field).endsWithIgnoreCase(value.toString());
    }

    private static BooleanExpression in(PathBuilder<?> path, String field, Object value) {

        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("IN 操作要求值必须是 Collection 类型");
        }
        if (CollectionUtils.isEmpty((Collection<?>) value)) {
            return Expressions.FALSE;
        }
        return path.get(field).in((Collection<?>) value);
    }

    private static BooleanExpression notIn(PathBuilder<?> path, String field, Object value) {

        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("NOT_IN 操作要求值必须是 Collection 类型");
        }
        if (CollectionUtils.isEmpty((Collection<?>) value)) {
            return Expressions.TRUE;
        }
        return path.get(field).notIn((Collection<?>) value);
    }

    private static BooleanExpression between(PathBuilder<?> path, String field, Object value) {

        List<?> bounds = extractBetweenBounds(value);
        return between(
                path,
                field,
                bounds.get(0),
                bounds.get(1)
        );
    }

    @SuppressWarnings("unchecked")
    private static BooleanExpression between(PathBuilder<?> path, String field, Object lowerBound, Object upperBound) {

        if (!(lowerBound instanceof Comparable) || !(upperBound instanceof Comparable)) {
            throw new IllegalArgumentException("BETWEEN 操作要求上下边界值必须实现 Comparable 接口");
        }
        if (!lowerBound.getClass().isInstance(upperBound)) {
            throw new IllegalArgumentException("BETWEEN 操作要求上下边界值类型一致");
        }
        return path.getComparable(
                        field,
                        (Class<Comparable>) lowerBound.getClass()
                )
                .between(
                        (Comparable<?>) lowerBound,
                        (Comparable<?>) upperBound
                );
    }

    private static List<?> extractBetweenBounds(Object value) {

        if (value instanceof List<?> list) {
            if (list.size() != 2) {
                throw new IllegalArgumentException("BETWEEN 操作要求值必须包含 2 个边界值");
            }
            return list;
        }

        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            if (length != 2) {
                throw new IllegalArgumentException("BETWEEN 操作要求值必须包含 2 个边界值");
            }
            List<Object> bounds = new ArrayList<>(2);
            bounds.add(Array.get(value, 0));
            bounds.add(Array.get(value, 1));
            return bounds;
        }

        throw new IllegalArgumentException("BETWEEN 操作要求值必须是有序 List 或数组类型");
    }

}
