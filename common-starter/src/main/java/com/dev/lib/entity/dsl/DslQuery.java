package com.dev.lib.entity.dsl;

import com.dev.lib.entity.BaseEntity;
import com.dev.lib.entity.dsl.core.EntityPathManager;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.PredicateAssembler;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.entity.dsl.group.LogicalOperator;
import com.dev.lib.web.model.QueryRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class DslQuery<E extends BaseEntity> {

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
    public Boolean deleted = false;

    @JsonIgnore
    @ConditionIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<QueryFieldMerger.FieldMetaValue> externalFields = new ArrayList<>();

    @JsonIgnore
    @ConditionIgnore
    protected LogicalOperator selfOperator = LogicalOperator.AND;

    @SuppressWarnings("unchecked")
    public EntityPathBase<E> getEntityPath() {
        return (EntityPathBase<E>) EntityPathManager.getEntityPath(
                FieldMetaCache.getMeta(getClass()).getEntityClass()
        );
    }

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
        if (pageRequest == null) {
            return Sort.unsorted();
        }
        return pageRequest.toSort();
    }

    public Pageable toPageable() {
        if (pageRequest == null) {
            return PageRequest.of(0, 500, Sort.unsorted());
        }
        return pageRequest.toPageable();
    }

    public static <E extends BaseEntity> Predicate toPredicate(
            DslQuery<E> query,
            BooleanExpression... expressions
    ) {
        if (query != null) {
            List<QueryFieldMerger.FieldMetaValue> self = QueryFieldMerger.resolve(query);
            Map<String, QueryFieldMerger.FieldMetaValue> fields = new HashMap<>();

            for (QueryFieldMerger.FieldMetaValue fieldMetaValue : self) {
                fields.put(fieldMetaValue.getFieldMeta().getTargetField(), fieldMetaValue);
            }
            query.getExternalFields().forEach(it ->
                    fields.put(it.getFieldMeta().getTargetField(), it)
            );

            return PredicateAssembler.assemble(query, fields.values(), expressions);
        }
        if (expressions.length == 0) {
            return null;
        }

        return PredicateAssembler.assemble(null, null, expressions);
    }
}