package com.dev.lib.mongo;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.DslQueryFieldResolver;
import com.dev.lib.entity.dsl.core.FieldMetaCache;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.mongo.dsl.PredicateAssembler;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.*;
import java.util.stream.StreamSupport;

@NoRepositoryBean
public interface BaseRepository<T extends MongoEntity>
        extends MongoRepository<T, String>, QuerydslPredicateExecutor<T> {

    int BATCH_SIZE = 256;

    // ==================== 批量写入（分批）====================

    @Override
    default <S extends T> List<S> saveAll(Iterable<S> entities) {

        List<S> result = new ArrayList<>();
        List<S> insertBatch = new ArrayList<>(BATCH_SIZE + 1);

        for (S entity : entities) {
            if (entity == null) continue;
            if (entity.isNew()) {
                insertBatch.add(entity);
                if (insertBatch.size() >= BATCH_SIZE) {
                    result.addAll(insert(insertBatch));
                    insertBatch = new ArrayList<>(BATCH_SIZE + 1);
                }
            } else {
                result.add(save(entity));
            }
        }
        if (!insertBatch.isEmpty()) {
            result.addAll(insert(insertBatch));
        }
        return result;
    }

    @Override
    default void deleteAllById(Iterable<? extends String> ids) {

        for (String id : ids) {
            if (id != null) {
                deleteById(id);
            }
        }
    }

    // ==================== DSL 查询 ====================

    default Optional<T> load(DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "load");
        Predicate predicate = toPredicate(query, expressions);
        var sort = query != null ? query.toSort(getAllowedFields(query)) : org.springframework.data.domain.Sort.unsorted();
        return findBy(predicate, q -> q.sortBy(sort).first());
    }

    default List<T> loads(DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "loads");
        Predicate predicate = toPredicate(query, expressions);

        if (query != null && query.getLimit() != null) {
            return findAll(predicate, query.toPageable(getAllowedFields(query))).getContent();
        }

        Iterable<T> result = query != null
                             ? findAll(predicate, query.toSort(getAllowedFields(query)))
                             : findAll(predicate);

        return StreamSupport.stream(result.spliterator(), false).toList();
    }

    default Page<T> page(DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "page");
        return findAll(
                toPredicate(
                        query,
                        expressions
                ),
                query.toPageable(getAllowedFields(query))
        );
    }

    default boolean exists(DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "exists");
        return exists(toPredicate(
                query,
                expressions
        ));
    }

    default long count(DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "count");
        return count(toPredicate(
                query,
                expressions
        ));
    }

    default void delete(DslQuery<T> query, BooleanExpression... expressions) {

        ensureNonAggregateQuery(query, "delete");
        Predicate predicate = toPredicate(query, expressions);
        Iterable<T> entities = findAll(predicate);
        deleteAll(entities);
    }

    private Predicate toPredicate(DslQuery<T> query, BooleanExpression... expressions) {

        if (query == null) {
            return PredicateAssembler.assemble(null, null, expressions);
        }

        Collection<QueryFieldMerger.FieldMetaValue> merged = DslQueryFieldResolver.resolveMerged(
                query,
                DslQueryFieldResolver.OverridePolicy.EXTERNAL_OVERRIDE_SELF
        );

        return PredicateAssembler.assemble(query, merged, expressions);
    }

    private Set<String> getAllowedFields(DslQuery<T> query) {

        if (query == null) return Collections.emptySet();
        return FieldMetaCache.getMeta(query.getClass()).entityFieldNames();
    }

    private void ensureNonAggregateQuery(DslQuery<T> query, String operation) {

        if (query != null && query.hasAgg()) {
            throw new IllegalStateException("检测到 agg() 聚合配置，" + operation + " 不支持聚合查询");
        }
    }

}
