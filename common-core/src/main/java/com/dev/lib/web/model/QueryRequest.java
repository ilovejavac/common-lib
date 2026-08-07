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
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

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

    private static final int MAX_SIZE = 1024;

    /**
     * 前端分页最大可见记录数
     */
    private static final int MAX_TOTAL_RECORDS = 65536;

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
     */
    public Pageable toPageable(Set<String> allowFields, Sort defaultSort) {

        int normalizedPage = normalizePage();
        int normalizedSize = normalizeSize();

        logPageRange(
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
     * 记录超出前端可见范围的分页请求
     */
    private void logPageRange(int page, int size) {
        // 计算偏移量：offset = (page - 1) * size
        long offset = (long) (page - 1) * size;

        if (offset >= MAX_TOTAL_RECORDS) {
            log.info(
                    "查询范围超出前端可见上限，将返回空页：maxRecords={}, offset={}, page={}, size={}",
                    MAX_TOTAL_RECORDS,
                    offset,
                    page,
                    size
            );
            return;
        }

        if (offset + size > MAX_TOTAL_RECORDS) {
            log.info(
                    "查询到达前端可见范围末尾：offset={}, size={}, maxRecords={}",
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
     * 获取前端分页最大可见记录数
     */
    public static int getMaxTotalRecords() {

        return MAX_TOTAL_RECORDS;
    }

    /**
     * 根据分页偏移量限制当前页对前端可见的记录数
     */
    public static int limitPageSize(long offset, int pageSize) {

        if (offset < 0) {
            throw new IllegalArgumentException("分页偏移量不能小于 0");
        }
        if (pageSize < 0) {
            throw new IllegalArgumentException("分页大小不能小于 0");
        }

        long remaining = Math.max(0L, MAX_TOTAL_RECORDS - offset);
        return (int) Math.min(pageSize, remaining);
    }

    /**
     * 限制前端分页返回的总记录数
     */
    public static long limitTotal(long total) {

        if (total < 0) {
            throw new IllegalArgumentException("总记录数不能小于 0");
        }
        return Math.min(total, MAX_TOTAL_RECORDS);
    }

	public <R> QueryRequest<R> map(Function<T, R> convert) {

		QueryRequest<R> request = new QueryRequest<>();

		BeanUtils.copyProperties(this, request);
		request.setQuery(convert.apply(query));

		return request;
	}

}
