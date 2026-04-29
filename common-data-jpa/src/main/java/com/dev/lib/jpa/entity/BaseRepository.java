package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.dsl.FieldSelector;
import com.dev.lib.jpa.entity.dsl.SFunction;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@NoRepositoryBean
public interface BaseRepository<T extends JpaEntity> extends JpaRepository<T, Long> {

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

    default QueryBuilder<T> select(SFunction<? super T, ?> field1) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(field1);
    }

    default QueryBuilder<T> select(SFunction<? super T, ?> field1, SFunction<? super T, ?> field2) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(field1, field2);
    }

    default QueryBuilder<T> select(SFunction<? super T, ?> field1, SFunction<? super T, ?> field2, SFunction<? super T, ?> field3) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(field1, field2, field3);
    }

    default QueryBuilder<T> select(
            SFunction<? super T, ?> field1,
            SFunction<? super T, ?> field2,
            SFunction<? super T, ?> field3,
            SFunction<? super T, ?> field4
    ) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(field1, field2, field3, field4);
    }

    default QueryBuilder<T> select(
            SFunction<? super T, ?> field1,
            SFunction<? super T, ?> field2,
            SFunction<? super T, ?> field3,
            SFunction<? super T, ?> field4,
            SFunction<? super T, ?> field5
    ) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(field1, field2, field3, field4, field5);
    }

    default QueryBuilder<T> select(
            SFunction<? super T, ?> field1,
            SFunction<? super T, ?> field2,
            SFunction<? super T, ?> field3,
            SFunction<? super T, ?> field4,
            SFunction<? super T, ?> field5,
            SFunction<? super T, ?> field6
    ) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(field1, field2, field3, field4, field5, field6);
    }

    @SuppressWarnings("unchecked")
    default QueryBuilder<T> select(FieldSelector<T>... selectors) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).select(selectors);
    }

    default QueryBuilder<T> select(String... fieldNames) {
        return new QueryBuilder<>(RepositoryUtils.unwrap(this)).selectByNames(fieldNames);
    }

    default UpdateBuilder<T> update() {
        return new UpdateBuilder<>(RepositoryUtils.unwrap(this));
    }

    default EtlSqlBuilder<T> etl(String sqlScript) {
        return new EtlSqlBuilder<>(RepositoryUtils.unwrap(this), sqlScript);
    }

    default DropTableBuilder<T> drop(String tableName) {
        return new DropTableBuilder<>(RepositoryUtils.unwrap(this), tableName);
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

    @Override
    default long count() {
        return count(null, new BooleanExpression[0]);
    }

    default long count(DslQuery<T> dslQuery) {
        return count(dslQuery, new BooleanExpression[0]);
    }

    long count(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default long count(BooleanExpression... expressions) {
        return count((DslQuery<T>) null, expressions);
    }

    boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default boolean exists(BooleanExpression... expressions) {
        return exists(null, expressions);
    }

    Stream<T> stream(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default Stream<T> stream(BooleanExpression... expressions) {
        return stream(null, expressions);
    }

    <R> List<R> aggregate(DslQuery<T> dslQuery, BooleanExpression... expressions);

    long delete(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default long delete(BooleanExpression... expressions) {
        return delete(null, expressions);
    }

    // ==================== 物理删除 ====================

    default PhysicalDeleteRepository<T> physicalDelete() {
        return new PhysicalDeleteRepository<>(this);
    }

    T ref(Long id);
}
