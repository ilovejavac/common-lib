package com.dev.lib.mongo;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.mongo.dsl.PredicateAssembler;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@NoRepositoryBean
public interface BaseRepository<T extends MongoEntity>
        extends MongoRepository<T, String>, QuerydslPredicateExecutor<T> {

    default Optional<T> load(DslQuery<T> query, BooleanExpression... expressions) {
        return findOne(toPredicate(query, expressions));
    }

    default List<T> loads(DslQuery<T> query, BooleanExpression... expressions) {
        Predicate predicate = toPredicate(query, expressions);

        if (query != null && query.getLimit() != null) {
            return findAll(predicate, query.toPageable(getAllowedFields(query))).getContent();
        }

        Iterable<T> result = query != null
                ? findAll(predicate, query.toSort(getAllowedFields(query)))
                : findAll(predicate);

        return StreamSupport.stream(result.spliterator(), false).collect(Collectors.toList());
    }

    default Page<T> page(DslQuery<T> query, BooleanExpression... expressions) {
        return findAll(toPredicate(query, expressions), query.toPageable(getAllowedFields(query)));
    }

    default boolean exists(DslQuery<T> query, BooleanExpression... expressions) {
        return exists(toPredicate(query, expressions));
    }

    default long count(DslQuery<T> query, BooleanExpression... expressions) {
        return count(toPredicate(query, expressions));
    }

    private Predicate toPredicate(DslQuery<T> query, BooleanExpression... expressions) {
        if (query == null) {
            return PredicateAssembler.assemble(null, null, expressions);
        }

        List<QueryFieldMerger.FieldMetaValue> fields = QueryFieldMerger.resolve(query);
        Map<String, QueryFieldMerger.FieldMetaValue> fieldMap = new HashMap<>();

        for (QueryFieldMerger.FieldMetaValue fv : fields) {
            String key = fv.getFieldMeta().targetField() + "-" + fv.getFieldMeta().queryType();
            fieldMap.put(key, fv);
        }

        query.getExternalFields().forEach(fv -> {
            String key = fv.getFieldMeta().targetField() + "-" + fv.getFieldMeta().queryType();
            fieldMap.put(key, fv);
        });

        return PredicateAssembler.assemble(query, fieldMap.values(), expressions);
    }

    private Set<String> getAllowedFields(DslQuery<T> query) {
        if (query == null) return Collections.emptySet();
        Class<?> entityClass = FieldMetaCache.getMeta(query.getClass()).entityClass();
        return Arrays.stream(entityClass.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }
}
