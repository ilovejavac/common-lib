package com.dev.lib.search.dsl;

import com.dev.lib.entity.dsl.QueryType;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

public final class ExpressionBuilder {

    private ExpressionBuilder() {
    }

    public static Query build(String field, QueryType type, Object value) {

        if (value == null && type != QueryType.IS_NULL && type != QueryType.IS_NOT_NULL) {
            return null;
        }

        return switch (type) {
            case EQ -> term(field, value);
            case NE -> mustNot(term(field, value));
            case GT -> range(field, "gt", value);
            case GE -> range(field, "gte", value);
            case LT -> range(field, "lt", value);
            case LE -> range(field, "lte", value);
            case LIKE -> match(field, value.toString());
            case START_WITH -> prefix(keywordField(field), value.toString());
            case END_WITH -> wildcard(keywordField(field), "*" + value);
            case IN -> terms(field, (Collection<?>) value);
            case NOT_IN -> mustNot(terms(field, (Collection<?>) value));
            case IS_NULL -> mustNot(exists(field));
            case IS_NOT_NULL -> exists(field);
            case BETWEEN -> {
                if (value instanceof Object[] arr && arr.length == 2) {
                    yield rangeBetween(field, arr[0], arr[1]);
                }
                yield null;
            }
            default -> null;
        };
    }

    private static Query term(String field, Object value) {
        String targetField = (value instanceof String) ? keywordField(field) : field;
        return Query.of(q -> q.term(t -> t.field(targetField).value(toFieldValue(value))));
    }

    private static Query terms(String field, Collection<?> values) {
        if (CollectionUtils.isEmpty(values)) return null;

        boolean isStringCollection = values.stream()
                .filter(v -> v != null)
                .findFirst()
                .map(v -> v instanceof String)
                .orElse(false);

        String targetField = isStringCollection ? keywordField(field) : field;
        List<FieldValue> fieldValues = values.stream()
                .map(ExpressionBuilder::toFieldValue)
                .toList();
        return Query.of(q -> q.terms(t -> t.field(targetField).terms(tv -> tv.value(fieldValues))));
    }

    private static Query match(String field, String value) {
        return Query.of(q -> q.match(m -> m.field(field).query(FieldValue.of(value))));
    }

    private static Query range(String field, String op, Object value) {
        return Query.of(q -> q.range(r -> {
            r.field(field);
            JsonData jsonValue = JsonData.of(value);
            return switch (op) {
                case "gt" -> r.gt(jsonValue);
                case "gte" -> r.gte(jsonValue);
                case "lt" -> r.lt(jsonValue);
                case "lte" -> r.lte(jsonValue);
                default -> r;
            };
        }));
    }

    private static Query rangeBetween(String field, Object from, Object to) {
        return Query.of(q -> q.range(r -> r
                .field(field)
                .gte(JsonData.of(from))
                .lte(JsonData.of(to))
        ));
    }

    private static Query wildcard(String field, String pattern) {
        return Query.of(q -> q.wildcard(w -> w
                .field(field)
                .value(pattern)
                .caseInsensitive(true)
        ));
    }

    private static Query prefix(String field, String value) {
        return Query.of(q -> q.prefix(p -> p.field(field).value(value)));
    }

    private static Query exists(String field) {
        return Query.of(q -> q.exists(e -> e.field(field)));
    }

    private static Query mustNot(Query query) {
        if (query == null) return null;
        return Query.of(q -> q.bool(b -> b.mustNot(query)));
    }

    private static String keywordField(String field) {
        if (field.endsWith(".keyword")) return field;
        return field + ".keyword";
    }

    private static FieldValue toFieldValue(Object value) {
        if (value == null) return FieldValue.NULL;
        if (value instanceof Number n) {
            if (n instanceof Double || n instanceof Float) {
                return FieldValue.of(n.doubleValue());
            }
            return FieldValue.of(n.longValue());
        }
        if (value instanceof Boolean b) return FieldValue.of(b);
        if (value instanceof Enum<?> e) return FieldValue.of(e.name());
        return FieldValue.of(value.toString());
    }
}
