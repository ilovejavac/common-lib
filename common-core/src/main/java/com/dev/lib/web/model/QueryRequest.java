package com.dev.lib.web.model;

import com.google.common.base.Strings;
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
    private static final Sort defaultSort = Sort.by(Sort.Direction.DESC, "createAt");

    // 游标
    private Cursor cursor;

    private T query;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(500)
    private Integer size = 20;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private String property;
        private Sort.Direction direction;
    }

    @Data
    public static class Cursor {
        private String key;
        private Sort.Direction direction = Sort.Direction.ASC;
    }

    public boolean hasCursor() {
        if (cursor == null) {
            return false;
        }
        return !Strings.isNullOrEmpty(cursor.key) && !cursor.key.isBlank() && cursor.direction != null;
    }

    /**
     * 排序方式
     * <p>
     * [{"property": "createAt", "direction": "DESC/ASC"}]
     */
    private List<Order> orders;

    public Sort toSort() {
        return orders == null || orders.isEmpty()
                ? defaultSort
                : Sort.by(orders.stream()
                .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                .toList());
    }

    public Pageable toPageable(Sort orElse) {
        return PageRequest.of(
                Math.max(0, Optional.ofNullable(page).orElse(1) - 1),
                Math.min(Optional.ofNullable(size).orElse(20), 500),
                toSort()
        );
    }
}