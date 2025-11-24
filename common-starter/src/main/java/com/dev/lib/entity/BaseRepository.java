package com.dev.lib.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.PredicateAssembler;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dev.lib.entity.dsl.DslQuery.toPredicate;

@NoRepositoryBean
@SuppressWarnings("all")
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, Long>, ListQuerydslPredicateExecutor<T> {

    default Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return findOne(toPredicate(dslQuery, expressions));
    }

    default List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return findAll(toPredicate(dslQuery, expressions), dslQuery.toSort());
    }

    default Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return findAll(toPredicate(dslQuery, expressions), dslQuery.toPageable());
    }

    default boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return exists(toPredicate(dslQuery, expressions));
    }

    default long count(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return count(toPredicate(dslQuery, expressions));
    }
}
