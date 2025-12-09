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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Slf4j
@Data
@NoArgsConstructor
public class QueryRequest<T> {
    private static final int MAX_PAGE = 200;
    private static final int MAX_SIZE = 50;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("id"));

    private T query;

    @Min(value = 1, message = "分页数不能小于 1")
    @Max(value = MAX_PAGE, message = "分页数不能大于 200")
    private Integer page = 1;

    @Min(value = 1, message = "页大小不能小于 1")
    @Max(value = MAX_SIZE, message = "页大小不能大于 50")
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
                .filter(o -> StringUtils.hasText(o.getProperty()))
                .filter(o -> o.getDirection() != null)
                .filter(o -> allowFields.contains(o.getProperty()))
                .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                .toList();

        return validOrders.isEmpty() ? DEFAULT_SORT : Sort.by(validOrders);
    }

    public Pageable toPageable(Set<String> allowFields) {
        return PageRequest.of(Math.min(page, MAX_PAGE) - 1, Math.min(size, MAX_SIZE), toSort(allowFields));
    }
}
