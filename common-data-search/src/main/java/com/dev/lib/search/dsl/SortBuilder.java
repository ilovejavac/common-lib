package com.dev.lib.search.dsl;

import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SortBuilder {

    private SortBuilder() {

    }

    public static List<SortOptions> build(Sort sort, Set<String> allowFields) {

        List<SortOptions> sortOptions = new ArrayList<>();

        if (sort == null || sort.isUnsorted()) {
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("id").order(SortOrder.Desc))));
            return sortOptions;
        }

        for (Sort.Order order : sort) {
            String field = order.getProperty();
            if (!allowFields.contains(field)) continue;

            SortOrder sortOrder = order.isAscending() ? SortOrder.Asc : SortOrder.Desc;
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field(field).order(sortOrder))));
        }

        if (sortOptions.isEmpty()) {
            sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("id").order(SortOrder.Desc))));
        }

        return sortOptions;
    }

}
