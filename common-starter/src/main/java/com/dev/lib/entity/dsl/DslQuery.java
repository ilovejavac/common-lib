package com.dev.lib.entity.dsl;

import com.dev.lib.entity.BaseEntity;
import com.dev.lib.entity.dsl.core.EntityPathManager;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
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
    private final EntityPathBase<E> entityPath;

    @JsonIgnore
    @ConditionIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Map<String, QueryFieldMerger.ExternalFieldInfo> externalFields;

    @JsonIgnore
    @ConditionIgnore
    protected LogicalOperator selfOperator = LogicalOperator.AND;

    @SuppressWarnings("unchecked")
    protected DslQuery() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            Class<E> entityClass = (Class<E>) parameterizedType.getActualTypeArguments()[0];
            this.entityPath = EntityPathManager.getEntityPath(entityClass);
        } else {
            throw new IllegalStateException("必须指定泛型类型");
        }
    }

    public DslQuery<E> with(Object query) {
        if (query != null) {
            this.externalFields = QueryFieldMerger.merge(this, query);
        }
        return this;
    }

    public DslQuery<E> with(QueryRequest<?> pageRequest) {
        this.pageRequest = pageRequest;
        if (pageRequest != null) {
            this.externalFields = QueryFieldMerger.merge(this, pageRequest.getQuery());
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
            return PageRequest.of(1, 500, Sort.unsorted());
        }
        return pageRequest.toPageable();
    }

    /**
     * 构建 Predicate
     */
    public static <E extends BaseEntity> Predicate toPredicate(
            DslQuery<E> query,
            BooleanExpression... expressions
    ) {
        Map<String, QueryFieldMerger.ExternalFieldInfo> externalFields =
                query != null && query.externalFields != null
                        ? query.externalFields
                        : Map.of();

        return PredicateAssembler.assemble(query, externalFields, expressions);
    }
}