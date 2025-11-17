package com.dev.lib.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.web.model.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, Long>, ListQuerydslPredicateExecutor<T> {

    default Optional<T> fetchOne(DslQuery<T> dslQuery) {
        if (dslQuery == null) {
            return Optional.empty();
        }
        return findOne(dslQuery.toPredicate());
    }

    default List<T> fetch(DslQuery<T> dslQuery) {
        if (dslQuery == null) {
            return findAll();
        }
        return findAll(dslQuery.toPredicate());
    }

    default <Q extends DslQuery<T>> List<T> fetch(QueryRequest<Q> qry) {
        if (qry == null || qry.getQuery() == null) {
            return qry == null ? List.of() : findAll(qry.toSort());
        }
        return findAll(qry.toPredicate(), qry.toSort());
    }

    default <Q extends DslQuery<T>> Page<T> page(QueryRequest<Q> qry) {
        if (qry == null) {
            return Page.empty();
        }
        if (qry.getQuery() == null) {
            return findAll(qry.toPageable());
        }
        return findAll(qry.toPredicate(), qry.toPageable());
    }
}
