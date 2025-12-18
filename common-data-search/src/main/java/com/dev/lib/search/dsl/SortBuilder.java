package com.dev.lib.search.dsl;

import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.springframework.data.domain.Sort;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SortBuilder {

    private SortBuilder() {

    }

    public static List<SortOptions> build(Sort sort, Map<String, Class<?>> fieldTypes) {

        List<SortOptions> sortOptions = new ArrayList<>();

        if (sort == null || sort.isUnsorted()) {
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc))));
            return sortOptions;
        }

        for (Sort.Order order : sort) {
            String field = order.getProperty();
            if (!fieldTypes.containsKey(field)) continue;

            String    sortField = resolveSortField(field, fieldTypes.get(field));
            SortOrder sortOrder = order.isAscending() ? SortOrder.Asc : SortOrder.Desc;
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field(sortField).order(sortOrder))));
        }

        if (sortOptions.isEmpty()) {
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc))));
        }

        return sortOptions;
    }

    private static String resolveSortField(String field, Class<?> type) {

        if (type == null) return field + ".keyword";

        // 数值、布尔、日期不加 .keyword
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) return field;
        if (type == Boolean.class || type == boolean.class) return field;
        if (Temporal.class.isAssignableFrom(type)) return field;
        if (type.isEnum()) return field;

        // String 及其他加 .keyword
        return field + ".keyword";
    }

}
