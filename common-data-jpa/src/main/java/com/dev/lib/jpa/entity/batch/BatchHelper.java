package com.dev.lib.jpa.entity.batch;

import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class BatchHelper {

    private BatchHelper() {
    }

    public static <T extends JpaEntity> List<Long> fetchIdsAfter(BaseRepositoryImpl<T> repository, Predicate basePredicate, Long lastId) {

        BooleanBuilder where = new BooleanBuilder(basePredicate);
        if (lastId != null) {
            where.and(repository.getIdPath().gt(lastId));
        }
        return repository.getQueryFactory().select(repository.getIdPath())
                .from(repository.getPath())
                .where(where)
                .orderBy(repository.getIdPath().asc())
                .limit(repository.getInClauseBatchSize())
                .fetch();
    }

    public static <T extends JpaEntity, E, K> void forEachBatch(
            BaseRepositoryImpl<T> repository,
            Iterable<? extends E> source,
            Function<? super E, ? extends K> idExtractor,
            Consumer<List<K>> batchAction
    ) {

        if (source == null) {
            return;
        }

        List<K> batch = new ArrayList<>(repository.getInClauseBatchSize() + 1);
        for (E item : source) {
            K key = idExtractor.apply(item);
            if (key == null) {
                continue;
            }
            batch.add(key);
            if (batch.size() >= repository.getInClauseBatchSize()) {
                batchAction.accept(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            batchAction.accept(batch);
        }
    }
}
