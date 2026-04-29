package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.dsl.FieldSelector;
import com.dev.lib.jpa.entity.dsl.SFunction;
import com.dev.lib.jpa.entity.dsl.SelectBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class QueryBuilder<T extends JpaEntity> {

    private final BaseRepositoryImpl<T> impl;

    private final QueryContext context = new QueryContext();

    private SelectBuilder<T> selectBuilder;

    public QueryBuilder(BaseRepositoryImpl<T> impl) {

        this.impl = impl;
    }

    public QueryBuilder<T> selectByNames(String... fieldNames) {

        this.selectBuilder = new SelectBuilder<>(impl.getEntityClass(), fieldNames);
        return this;
    }

    // ==================== 上下文配置 ====================

    public QueryBuilder<T> lockForUpdate() {

        context.lockForUpdate();
        return this;
    }

    public QueryBuilder<T> lockForShare() {

        context.lockForShare();
        return this;
    }

    public QueryBuilder<T> skipLocked() {

        context.skipLocked();
        return this;
    }

    public QueryBuilder<T> withDeleted() {

        context.withDeleted();
        return this;
    }

    public QueryBuilder<T> onlyDeleted() {

        context.onlyDeleted();
        return this;
    }

    // ==================== 字段选择 ====================

    public QueryBuilder<T> select(SFunction<? super T, ?> field1) {

        return selectWithDefaults(List.of(field1));
    }

    public QueryBuilder<T> select(SFunction<? super T, ?> field1, SFunction<? super T, ?> field2) {

        return selectWithDefaults(List.of(field1, field2));
    }

    public QueryBuilder<T> select(
            SFunction<? super T, ?> field1,
            SFunction<? super T, ?> field2,
            SFunction<? super T, ?> field3
    ) {

        return selectWithDefaults(List.of(field1, field2, field3));
    }

    public QueryBuilder<T> select(
            SFunction<? super T, ?> field1,
            SFunction<? super T, ?> field2,
            SFunction<? super T, ?> field3,
            SFunction<? super T, ?> field4
    ) {

        return selectWithDefaults(List.of(field1, field2, field3, field4));
    }

    public QueryBuilder<T> select(
            SFunction<? super T, ?> field1,
            SFunction<? super T, ?> field2,
            SFunction<? super T, ?> field3,
            SFunction<? super T, ?> field4,
            SFunction<? super T, ?> field5
    ) {

        return selectWithDefaults(List.of(field1, field2, field3, field4, field5));
    }

    public QueryBuilder<T> select(
            SFunction<? super T, ?> field1,
            SFunction<? super T, ?> field2,
            SFunction<? super T, ?> field3,
            SFunction<? super T, ?> field4,
            SFunction<? super T, ?> field5,
            SFunction<? super T, ?> field6
    ) {

        return selectWithDefaults(List.of(field1, field2, field3, field4, field5, field6));
    }

    @SafeVarargs
    public final QueryBuilder<T> select(FieldSelector<T>... selectors) {

        this.selectBuilder = new SelectBuilder<>(impl.getEntityClass(), List.of(selectors));
        return this;
    }

    // ==================== 终结操作 - 返回实体 ====================

    public Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.load(context, selectBuilder, dslQuery, expressions);
    }

    public List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.loads(context, selectBuilder, dslQuery, expressions);
    }

    public Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.page(context, selectBuilder, dslQuery, expressions);
    }

    public Stream<T> stream(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.stream(context, selectBuilder, dslQuery, expressions);
    }

    // ==================== 终结操作 - 返回 DTO ====================

    public <D> Optional<D> load(Class<D> dtoClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        requireSelect();
        return impl.load(context, selectBuilder, dtoClass, dslQuery, expressions);
    }

    public <D> List<D> loads(Class<D> dtoClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        requireSelect();
        return impl.loads(context, selectBuilder, dtoClass, dslQuery, expressions);
    }

    public <D> Page<D> page(Class<D> dtoClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        requireSelect();
        return impl.page(context, selectBuilder, dtoClass, dslQuery, expressions);
    }

    public <D> Stream<D> stream(Class<D> dtoClass, DslQuery<T> dslQuery, BooleanExpression... expressions) {

        requireSelect();
        return impl.stream(context, selectBuilder, dtoClass, dslQuery, expressions);
    }

    public <D> Stream<D> stream(Class<D> dtoClass, BooleanExpression... expressions) {

        return stream(dtoClass, null, expressions);
    }

    // ==================== 终结操作 - 聚合 ====================

    public long count(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.count(context, dslQuery, expressions);
    }

    public long count(BooleanExpression... expressions) {

        return count(null, expressions);
    }

    public boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.exists(context, dslQuery, expressions);
    }

    public boolean exists(BooleanExpression... expressions) {

        return exists(null, expressions);
    }

    // ==================== 终结操作 - 删除 ====================

    public long delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.delete(context, dslQuery, expressions);
    }

    public long delete(BooleanExpression... expressions) {

        return delete(null, expressions);
    }

    // ==================== 内部 ====================

    private void requireSelect() {

        if (selectBuilder == null || selectBuilder.isEmpty()) {
            throw new IllegalStateException("返回 DTO 必须先调用 select()");
        }
    }

    private QueryBuilder<T> selectWithDefaults(List<SFunction<? super T, ?>> fields) {

        java.util.LinkedHashMap<String, FieldSelector<T>> selectors = new java.util.LinkedHashMap<>();
        for (SFunction<? super T, ?> field : fields) {
            FieldSelector<T> selector = FieldSelector.of(field);
            selectors.putIfAbsent(selector.getPath(), selector);
        }

        appendDefaultSelector(selectors, "bizId", String.class);
        appendDefaultSelector(selectors, "createdAt", java.time.LocalDateTime.class);
        appendDefaultSelector(selectors, "updatedAt", java.time.LocalDateTime.class);
        appendDefaultSelector(selectors, "creatorId", Long.class);
        appendDefaultSelector(selectors, "modifierId", Long.class);

        this.selectBuilder = new SelectBuilder<>(impl.getEntityClass(), List.copyOf(selectors.values()));
        return this;
    }

    private static <T extends JpaEntity> void appendDefaultSelector(
            java.util.LinkedHashMap<String, FieldSelector<T>> selectors,
            String path,
            Class<?> fieldType
    ) {

        selectors.putIfAbsent(path, new FieldSelector<>(path, fieldType, List.of()));
    }

}
