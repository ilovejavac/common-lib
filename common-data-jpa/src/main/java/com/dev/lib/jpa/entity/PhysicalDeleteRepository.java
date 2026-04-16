package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.TransactionHelper;
import com.querydsl.core.types.dsl.BooleanExpression;

public class PhysicalDeleteRepository<T extends JpaEntity> {

    private final BaseRepositoryImpl<T> impl;

    PhysicalDeleteRepository(BaseRepository<T> repository) {

        this.impl = RepositoryUtils.unwrap(repository);
    }

    public void delete(T entity) {

        TransactionHelper.runWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> impl.hardDelete(entity));
    }

    public void deleteById(Long id) {

        TransactionHelper.runWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> impl.hardDeleteById(id));
    }

    public void deleteAll(Iterable<? extends T> entities) {

        TransactionHelper.runWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> impl.hardDeleteAll(entities));
    }

    public void deleteAllById(Iterable<Long> ids) {

        TransactionHelper.runWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> impl.hardDeleteAllById(ids));
    }

    public long delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return TransactionHelper.callWithEntityManagerFactory(
                impl.getEntityManagerFactory(),
                () -> impl.hardDelete(dslQuery, expressions)
        );
    }

    public long delete(BooleanExpression... expressions) {

        return TransactionHelper.callWithEntityManagerFactory(
                impl.getEntityManagerFactory(),
                () -> impl.hardDelete(null, expressions)
        );
    }

}
