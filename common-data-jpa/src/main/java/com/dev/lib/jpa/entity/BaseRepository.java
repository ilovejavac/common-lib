package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoRepositoryBean
@SuppressWarnings("all")
public interface BaseRepository<T extends JpaEntity> extends JpaRepository<T, Long>, ListQuerydslPredicateExecutor<T> {

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

    private static <E extends JpaEntity> Predicate toPredicate(
            DslQuery<E> query,
            BooleanExpression... expressions
    ) {
        if (query != null) {
            List<QueryFieldMerger.FieldMetaValue> self = QueryFieldMerger.resolve(query);
            Map<String, QueryFieldMerger.FieldMetaValue> fields = new HashMap<>();

            for (QueryFieldMerger.FieldMetaValue fieldMetaValue : self) {
                fields.put(fieldMetaValue.getFieldMeta().targetField(), fieldMetaValue);
            }
            query.getExternalFields().forEach(it ->
                    fields.put(it.getFieldMeta().targetField(), it)
            );

            return PredicateAssembler.assemble(query, fields.values(), expressions);
        }
        if (expressions.length == 0) {
            return null;
        }

        return PredicateAssembler.assemble(null, null, expressions);
    }
}
