package com.dev.lib.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.web.model.QueryRequest;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

import static com.dev.lib.entity.dsl.DslQuery.toPredicate;

@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, Long>, ListQuerydslPredicateExecutor<T> {

    default Optional<T> fetchOne(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return findOne(toPredicate(dslQuery, expressions));
    }

    default List<T> fetch(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return findAll(toPredicate(dslQuery, expressions));
    }

    default <Q extends DslQuery<T>> List<T> fetch(QueryRequest<Q> qry, BooleanExpression... expressions) {
        if (qry == null) {
            return List.of();
        }
        return findAll(qry.toPredicate(expressions), qry.toSort());
    }

    default <Q extends DslQuery<T>> Page<T> page(QueryRequest<Q> qry, BooleanExpression... expressions) {
        if (qry == null) {
            return Page.empty();
        }
        return findAll(qry.toPredicate(expressions), qry.toPageable());
    }
}
