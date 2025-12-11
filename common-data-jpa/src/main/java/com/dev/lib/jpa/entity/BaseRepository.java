package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.jpa.entity.dsl.PredicateAssembler;
import com.dev.lib.util.StringUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@NoRepositoryBean
@SuppressWarnings("all")
public interface BaseRepository<T extends JpaEntity> extends JpaRepository<T, Long>, ListQuerydslPredicateExecutor<T> {

    default Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return findOne(toPredicate(dslQuery, expressions));
    }

    default List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        if (dslQuery.getLimit() != null) {
            return page(dslQuery, expressions).getContent();
        }
        Class<?> entityClass = FieldMetaCache.getMeta(dslQuery.getClass()).entityClass();
        Set<String> allowFields =
                Arrays.stream(entityClass.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());

        return findAll(toPredicate(dslQuery, expressions), dslQuery.toSort(allowFields));
    }

    default Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        Class<?> entityClass = FieldMetaCache.getMeta(dslQuery.getClass()).entityClass();
        Set<String> allowFields =
                Arrays.stream(entityClass.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
        return findAll(toPredicate(dslQuery, expressions), dslQuery.toPageable(allowFields));
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
                fields.put(
                        StringUtils.format(
                                "{}-{}",
                                fieldMetaValue.getFieldMeta().targetField(),
                                fieldMetaValue.getFieldMeta().queryType()
                        ), fieldMetaValue
                );
            }
            query.getExternalFields().forEach(it ->
                    fields.put(
                            StringUtils.format(
                                    "{}-{}",
                                    it.getFieldMeta().targetField(),
                                    it.getFieldMeta().queryType()
                            ), it
                    )
            );

            return PredicateAssembler.assemble(query, fields.values(), expressions);
        }
        if (expressions.length == 0) {
            return null;
        }

        return PredicateAssembler.assemble(null, null, expressions);
    }
}
