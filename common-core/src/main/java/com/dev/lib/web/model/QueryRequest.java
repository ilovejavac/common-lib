package com.dev.lib.web.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

/**
 * 通用分页查询请求
 *
 * @param <T> 查询条件类型
 */
@Slf4j
@Data
@NoArgsConstructor
public class QueryRequest<T> {

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_SIZE = 20;

    private static final int MAX_SIZE = 64;

    /**
     * 最大可查询的总记录数（防止深度翻页）
     */
    private static final int MAX_TOTAL_RECORDS = 50000;

    /**
     * 查询条件
     */
    @NotNull
    @Valid
    private T query;

    /**
     * 页码（从 1 开始）
     */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer page = DEFAULT_PAGE;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "页大小不能小于 1")
    @Max(value = MAX_SIZE, message = "页大小不能大于 " + MAX_SIZE)
    private Integer size = DEFAULT_SIZE;

    /**
     * 排序规则列表
     */
    @Valid
    @Size(max = 10)
    private List<Order> orderBy;

    /**
     * 排序规则
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {

        @NotBlank(message = "排序字段不能为空")
        private String property;

        private Sort.Direction direction;

    }

    /**
     * 转换为 Spring Data Pageable
     *
     * @param allowFields 允许排序的字段白名单
     * @return 分页对象
     * @throws IllegalArgumentException 如果偏移量超过最大限制
     */
    public Pageable toPageable(Set<String> allowFields) {

        return toPageable(allowFields, Sort.by(Sort.Order.desc("id")));
    }

    /**
     * 转换为 Spring Data Pageable（带默认排序）
     *
     * @param allowFields 允许排序的字段白名单
     * @param defaultSort 默认排序
     * @return 分页对象
     * @throws IllegalArgumentException 如果偏移量超过最大限制
     */
    public Pageable toPageable(Set<String> allowFields, Sort defaultSort) {

        int normalizedPage = normalizePage();
        int normalizedSize = normalizeSize();

        // 🔒 检查总偏移量是否超过限制
        validateTotalRecords(
                normalizedPage,
                normalizedSize
        );

        return PageRequest.of(
                normalizedPage - 1,  // Spring Data 页码从 0 开始
                normalizedSize,
                toSort(allowFields, defaultSort)
        );
    }

    /**
     * 验证总记录数限制
     *
     * @throws IllegalArgumentException 如果超过限制
     */
    private void validateTotalRecords(int page, int size) {
        // 计算偏移量：offset = (page - 1) * size
        long offset = (long) (page - 1) * size;

        if (offset >= MAX_TOTAL_RECORDS) {
            String message = String.format(
                    "查询范围超出限制：最多只能查看前 %d 条数据，当前请求偏移量为 %d (page=%d, size=%d)",
                    MAX_TOTAL_RECORDS,
                    offset,
                    page,
                    size
            );
            log.warn(message);
            throw new IllegalArgumentException(message);
        }

        // 可选：也记录接近限制的情况
        if (offset + size > MAX_TOTAL_RECORDS) {
            log.info(
                    "查询接近限制：offset={}, size={}, maxRecords={}",
                    offset,
                    size,
                    MAX_TOTAL_RECORDS
            );
        }
    }

    /**
     * 构建排序对象（默认 id 降序[创建时间降序]）
     */
    public Sort toSort(Set<String> allowFields) {

        return toSort(allowFields, Sort.by(Sort.Order.desc("id")));
    }

    /**
     * 构建排序对象（带默认排序）
     */
    public Sort toSort(Set<String> allowFields, Sort defaultSort) {

        if (orderBy == null || orderBy.isEmpty()) {
            return defaultSort;
        }

        List<Sort.Order> validOrders = orderBy.stream()
                .filter(o -> StringUtils.hasText(o.getProperty()))
                .filter(o -> o.getDirection() != null)
                .filter(o -> allowFields.contains(o.getProperty()))
                .map(o -> new Sort.Order(
                        o.getDirection(),
                        o.getProperty()
                )).toList();

        return validOrders.isEmpty() ? defaultSort : Sort.by(validOrders);
    }

    /**
     * 标准化页码
     */
    private int normalizePage() {

        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /**
     * 标准化页大小
     */
    private int normalizeSize() {

        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * 获取最大可查询记录数（用于前端提示）
     */
    public static int getMaxTotalRecords() {

        return MAX_TOTAL_RECORDS;
    }

}