package com.dev.lib.web.model;

import jakarta.validation.constraints.Max;
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

    private List<Order> orders = List.of(new Order("createdAt", Sort.Direction.DESC));

    public Sort toSort() {
        return orders == null || orders.isEmpty()
                ? Sort.unsorted()
                : Sort.by(orders.stream()
                .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                .toList());
    }

    public Pageable toPageable() {
        return PageRequest.of(
                Math.max(0, page - 1),
                Math.min(size, 1000),
                toSort()
        );
    }
}