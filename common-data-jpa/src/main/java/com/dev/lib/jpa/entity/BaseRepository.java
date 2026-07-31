package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T extends JpaEntity> extends Repository<T, Long> {

    <S extends T> S save(S entity);

    <S extends T> List<S> saveAll(Iterable<S> entities);

    void delete(T entity);

    Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default Optional<T> load(BooleanExpression... expressions) {

        return load((DslQuery<T>) null, expressions);
    }

    List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default List<T> loads(BooleanExpression... expressions) {

        return loads(null, expressions);
    }

    Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions);

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
