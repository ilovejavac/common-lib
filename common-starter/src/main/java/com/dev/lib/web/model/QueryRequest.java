package com.dev.lib.web.model;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
public class QueryRequest<T> {
    private T query;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;

    private List<Orders> orders;

    @Data
    public static class Orders {
        private String property;
        private Sort.Direction direction;
    }

    public Pageable toPageable() {
        Sort sort = orders == null || orders.isEmpty()
                ? Sort.unsorted()
                : Sort.by(orders.stream()
                .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                .toList());

        return org.springframework.data.domain.PageRequest.of(page - 1, size, sort);
    }
}