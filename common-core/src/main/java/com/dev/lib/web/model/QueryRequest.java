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
import java.util.Optional;

@Slf4j
@Data
@NoArgsConstructor
public class QueryRequest<T> {
    private T query;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(1000)
    private Integer size = 20;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private String property;
        private Sort.Direction direction;
    }

    /**
     * 排序方式
     *
     * [{"property": "createAt", "direction": "DESC/ASC"}]
     */
    private List<Order> orders;

    public Sort toSort() {
        return orders == null || orders.isEmpty()
                ? Sort.by(Sort.Order.desc("createdAt"))
                : Sort.by(orders.stream()
                .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                .toList());
    }

    public Pageable toPageable() {
        log.info("{}", this);
        return PageRequest.of(
                Math.max(0, Optional.ofNullable(page).orElse(1) - 1),
                Math.min(Optional.ofNullable(size).orElse(20), 1000),
                toSort()
        );
    }
}