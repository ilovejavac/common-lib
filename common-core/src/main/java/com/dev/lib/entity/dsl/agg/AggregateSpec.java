package com.dev.lib.entity.dsl.agg;

import com.dev.lib.entity.dsl.QueryType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class AggregateSpec<E, R> {

    private final Class<R> targetClass;

    private final List<String> groupByFields = new ArrayList<>();

    private final List<Item> items = new ArrayList<>();

    private final List<Having> havings = new ArrayList<>();

    private final List<Order> orders = new ArrayList<>();

    private Integer offset;

    private Integer limit;

    private AggJoinStrategy joinStrategy = AggJoinStrategy.AUTO;

    public AggregateSpec(Class<R> targetClass) {

        this.targetClass = targetClass;
    }

    public List<String> getGroupByFields() {

        return Collections.unmodifiableList(groupByFields);
    }

    public List<Item> getItems() {

        return Collections.unmodifiableList(items);
    }

    public List<Having> getHavings() {

        return Collections.unmodifiableList(havings);
    }

    public List<Order> getOrders() {

        return Collections.unmodifiableList(orders);
    }

    void addGroupBy(String sourceField) {

        if (sourceField != null && !sourceField.isBlank()) {
            groupByFields.add(sourceField);
        }
    }

    void addItem(AggType type, String sourceField, String targetField) {

        if (sourceField == null || sourceField.isBlank()) {
            throw new IllegalArgumentException("聚合源字段不能为空");
        }
        if (targetField == null || targetField.isBlank()) {
            throw new IllegalArgumentException("聚合目标字段不能为空");
        }
        items.add(new Item(type, sourceField, targetField));
    }

    void addHaving(String targetField, QueryType queryType, Object value) {

        if (targetField == null || targetField.isBlank()) {
            throw new IllegalArgumentException("having 目标字段不能为空");
        }
        if (queryType == null || queryType == QueryType.EMPTY) {
            throw new IllegalArgumentException("having 查询类型不能为空");
        }
        havings.add(new Having(targetField, queryType, value));
    }

    void addOrder(String targetField, boolean asc) {

        if (targetField == null || targetField.isBlank()) {
            throw new IllegalArgumentException("order 目标字段不能为空");
        }
        orders.add(new Order(targetField, asc));
    }

    void setOffset(Integer offset) {

        if (offset == null) {
            this.offset = null;
            return;
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset 不能小于 0");
        }
        this.offset = offset;
    }

    void setLimit(Integer limit) {

        if (limit == null) {
            this.limit = null;
            return;
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit 必须大于 0");
        }
        this.limit = limit;
    }

    void setJoinStrategy(AggJoinStrategy joinStrategy) {

        this.joinStrategy = joinStrategy == null ? AggJoinStrategy.AUTO : joinStrategy;
    }

    public boolean isEmpty() {

        return items.isEmpty();
    }

    public record Item(AggType type, String sourceField, String targetField) {
    }

    public record Having(String targetField, QueryType queryType, Object value) {
    }

    public record Order(String targetField, boolean asc) {
    }
}
