package com.dev.lib.web.model;

import com.dev.lib.entity.dsl.DslQuery;
import com.querydsl.core.types.Predicate;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest<T extends DslQuery<?>> {
    private T query;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;

    private List<Orders> orders;

    public QueryRequest(T query) {
        this.query = query;
    }

    @Data
    public static class Orders {
        private String property;
        private Sort.Direction direction;
    }

    public Predicate toPredicate() {
        return query.toPredicate();
    }

    public Sort toSort() {
        return orders == null || orders.isEmpty()
                ? Sort.unsorted()
                : Sort.by(orders.stream()
                .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                .toList());
    }

    public Pageable toPageable() {
        Sort sort = toSort();

        return PageRequest.of(page - 1, size, sort);
    }
}