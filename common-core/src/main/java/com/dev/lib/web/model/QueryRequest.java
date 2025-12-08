package com.dev.lib.web.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

@Slf4j
@Data
@NoArgsConstructor
public class QueryRequest<T> {
    private static final int MAX_SIZE = 200;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("id"));

    private T query;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(MAX_SIZE)
    private Integer size = 20;

    private List<Order> orderBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private String property;
        private Sort.Direction direction;
    }

    public Sort toSort(Set<String> allowFields) {
        if (orderBy == null || orderBy.isEmpty()) {
            return DEFAULT_SORT;
        }

        List<Sort.Order> validOrders = orderBy.stream()
                .filter(o -> allowFields.contains(o.getProperty()))
                .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                .toList();

        return validOrders.isEmpty() ? DEFAULT_SORT : Sort.by(validOrders);
    }

    public Pageable toPageable(Set<String> allowFields) {
        return PageRequest.of(
                Math.max(1, page) - 1,
                Math.min(size, MAX_SIZE),
                toSort(allowFields)
        );
    }
}
