package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.dsl.FieldSelector;
import com.dev.lib.jpa.entity.dsl.SFunction;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@NoRepositoryBean
public interface BaseRepository<T extends JpaEntity> extends JpaRepository<T, Long>, ListQuerydslPredicateExecutor<T> {

    // ==================== 构建器入口 ====================

    default QueryBuilder<T> lockForUpdate() {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).lockForUpdate();
    }

    default QueryBuilder<T> lockForShare() {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).lockForShare();
    }

    default QueryBuilder<T> withDeleted() {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).withDeleted();
    }

    default QueryBuilder<T> onlyDeleted() {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).onlyDeleted();
    }

    @SuppressWarnings("unchecked")
    default QueryBuilder<T> select(SFunction<T, ?>... fields) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(fields);
    }

    @SuppressWarnings("unchecked")
    default QueryBuilder<T> select(FieldSelector<T>... selectors) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(selectors);
    }

    default QueryBuilder<T> select(String... fieldNames) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).selectByNames(fieldNames);
    }

    // ==================== 直接查询 ====================

    Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default Optional<T> load(BooleanExpression... expressions) {
        return load(null, expressions);
    }

    List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default List<T> loads(BooleanExpression... expressions) {
        return loads(null, expressions);
    }

    Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions);

    long count(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default long count(BooleanExpression... expressions) {
        return count(null, expressions);
    }

    boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default boolean exists(BooleanExpression... expressions) {
        return exists(null, expressions);
    }

    Stream<T> stream(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default Stream<T> stream(BooleanExpression... expressions) {
        return stream(null, expressions);
    }

    void delete(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default void delete(BooleanExpression... expressions) {
        delete(null, expressions);
    }

    // ==================== 物理删除 ====================

    default PhysicalDeleteRepository<T> physicalDelete() {
        return new PhysicalDeleteRepository<>(this);
    }
}
