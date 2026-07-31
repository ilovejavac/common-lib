package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.Bo;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@NoRepositoryBean
public interface BaseRepository<T extends JpaEntity> extends Repository<T, Long> {

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

    default PhysicalDeleteRepository<T> physicalDelete() {

        return new PhysicalDeleteRepository<>(RepositoryUtils.unwrap(this));
    }

    <S extends T> S save(S entity);

    default T save(Bo<T> bo) {

        T po = save(bo.getEntity());
        bo.emit();
        return po;
    }

    <S extends T> List<S> saveAll(Iterable<S> entities);

    default List<T> saveAll(Collection<? extends Bo<T>> bos) {

        return saveAll(bos.stream().map(Bo::getEntity).toList());
    }

    void delete(T entity);

    default void delete(Bo<T> bo) {

        bo.takeif(this::delete);
        bo.emit();
    }

    Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default Optional<T> load(BooleanExpression... expressions) {

        return load((DslQuery<T>) null, expressions);
    }

    List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default List<T> loads(BooleanExpression... expressions) {

        return loads(null, expressions);
    }

    Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default long count() {

        return count((DslQuery<T>) null, new BooleanExpression[0]);
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

    long delete(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default long delete(BooleanExpression... expressions) {

        return delete((DslQuery<T>) null, expressions);
    }

    Optional<T> loadForUpdate(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default Optional<T> loadForUpdate(BooleanExpression... expressions) {

        return loadForUpdate((DslQuery<T>) null, expressions);
    }

    UpdateBuilder<T> update();
}
