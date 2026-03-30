package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.TransactionHelper;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PhysicalDeleteRepository<T extends JpaEntity> {

    private final BaseRepository<T> repository;

    private BaseRepositoryImpl<T> getImpl() {

        return RepositoryUtils.unwrap(repository);
    }

    public void delete(T entity) {

        TransactionHelper.run(() -> getImpl().hardDelete(entity));
    }

    public void deleteById(Long id) {

        TransactionHelper.run(() -> getImpl().hardDeleteById(id));
    }

    public void deleteAll(Iterable<? extends T> entities) {

        getImpl().hardDeleteAll(entities);
    }

    public void deleteAllById(Iterable<Long> ids) {

        getImpl().hardDeleteAllById(ids);
    }

    public long delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return TransactionHelper.call(() -> getImpl().hardDelete(dslQuery, expressions));
    }

    public long delete(BooleanExpression... expressions) {

        return TransactionHelper.call(() -> getImpl().hardDelete(null, expressions));
    }

}
