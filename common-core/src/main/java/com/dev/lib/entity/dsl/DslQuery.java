package com.dev.lib.entity.dsl;

import com.dev.lib.entity.CoreEntity;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.dev.lib.web.model.QueryRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public abstract class DslQuery<E extends CoreEntity> {

    @ConditionIgnore
    private QueryRequest<?> pageRequest;

    @Condition(type = QueryType.EQ, field = "bizId")
    public String id;

    @Condition(type = QueryType.GE, field = "createAt")
    public LocalDateTime createStart;

    @Condition(type = QueryType.LE, field = "createAt")
    public LocalDateTime createEnd;

    @Condition(type = QueryType.EQ)
    public String createdBy;

    @Condition(type = QueryType.EQ)
    public String updatedBy;

    @Condition(type = QueryType.EQ)
    public Boolean deleted;

    @ConditionIgnore
    public String sortStr = "";
    public Integer start;
    public Integer limit;

    @JsonIgnore
    @ConditionIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<QueryFieldMerger.FieldMetaValue> externalFields = new ArrayList<>();

    @JsonIgnore
    @ConditionIgnore
    protected LogicalOperator selfOperator = LogicalOperator.AND;

    public DslQuery<E> external(Object query) {
        if (query != null) {
            this.externalFields.addAll(QueryFieldMerger.resolve(query));
        }
        return this;
    }

    public DslQuery<E> external(QueryRequest<?> pageRequest) {
        this.pageRequest = pageRequest;
        if (pageRequest != null) {
            this.externalFields.addAll(QueryFieldMerger.resolve(pageRequest.getQuery()));
        }
        return this;
    }

    public Sort toSort() {
        if (pageRequest == null || CollectionUtils.isEmpty(pageRequest.getOrders())) {
            if (sortStr == null || sortStr.isBlank()) {
                return Sort.by(Sort.Order.asc("id"));
            }

            List<Sort.Order> orders = Arrays.stream(sortStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        String[] parts = s.split("_");
                        String field = parts[0];
                        Sort.Direction dir = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                        return new Sort.Order(dir, field);
                    })
                    .toList();

            return orders.isEmpty() ? Sort.by(Sort.Order.asc("id")) : Sort.by(orders);
        }
        return pageRequest.toSort();
    }

    public Pageable toPageable() {
        if (pageRequest == null) {
            return PageRequest.of(
                    Optional.ofNullable(start).orElse(0),
                    Math.min(1000, Optional.ofNullable(limit).orElse(1000)),
                    Sort.by(Sort.Direction.ASC, "id")
            );
        }
        return pageRequest.toPageable();
    }
}