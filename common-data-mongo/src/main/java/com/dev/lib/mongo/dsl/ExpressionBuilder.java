package com.dev.lib.mongo.dsl;

import com.dev.lib.entity.dsl.QueryType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

public final class ExpressionBuilder {

    private ExpressionBuilder() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BooleanExpression build(
            PathBuilder<?> pathBuilder,
            String field,
            QueryType type,
            Object value
    ) {
        if (value == null && type != QueryType.IS_NULL && type != QueryType.IS_NOT_NULL) {
            return null;
        }

        // 直接使用完整路径，不要逐级 get()
        return switch (type) {
            case EQ -> pathBuilder.get(field).eq(value);
            case NE -> pathBuilder.get(field).ne(value);
            case GT -> pathBuilder.getComparable(field, (Class<Comparable>) value.getClass()).gt((Comparable) value);
            case GE -> pathBuilder.getComparable(field, (Class<Comparable>) value.getClass()).goe((Comparable) value);
            case LT -> pathBuilder.getComparable(field, (Class<Comparable>) value.getClass()).lt((Comparable) value);
            case LE -> pathBuilder.getComparable(field, (Class<Comparable>) value.getClass()).loe((Comparable) value);
            case LIKE -> pathBuilder.getString(field).containsIgnoreCase(value.toString());
            case START_WITH -> pathBuilder.getString(field).startsWithIgnoreCase(value.toString());
            case END_WITH -> pathBuilder.getString(field).endsWithIgnoreCase(value.toString());
            case IN -> {
                Collection<?> coll = (Collection<?>) value;
                yield CollectionUtils.isEmpty(coll) ? null : pathBuilder.get(field).in(coll);
            }
            case NOT_IN -> {
                Collection<?> coll = (Collection<?>) value;
                yield CollectionUtils.isEmpty(coll) ? null : pathBuilder.get(field).notIn(coll);
            }
            case IS_NULL -> pathBuilder.get(field).isNull();
            case IS_NOT_NULL -> pathBuilder.get(field).isNotNull();
            default -> null;
        };
    }
}
