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

        BaseRepositoryImpl<T> impl = getImpl();
        TransactionHelper.runWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> impl.hardDelete(entity));
    }

    public void deleteById(Long id) {

        BaseRepositoryImpl<T> impl = getImpl();
        TransactionHelper.runWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> impl.hardDeleteById(id));
    }

    public void deleteAll(Iterable<? extends T> entities) {

        BaseRepositoryImpl<T> impl = getImpl();
        TransactionHelper.runWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> impl.hardDeleteAll(entities));
    }

    public void deleteAllById(Iterable<Long> ids) {

        BaseRepositoryImpl<T> impl = getImpl();
        TransactionHelper.runWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> impl.hardDeleteAllById(ids));
    }

    public long delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        BaseRepositoryImpl<T> impl = getImpl();
        return TransactionHelper.callWithEntityManagerFactory(
                impl.getEntityManagerFactory(),
                () -> impl.hardDelete(dslQuery, expressions)
        );
    }

    public long delete(BooleanExpression... expressions) {

        BaseRepositoryImpl<T> impl = getImpl();
        return TransactionHelper.callWithEntityManagerFactory(
                impl.getEntityManagerFactory(),
                () -> impl.hardDelete(null, expressions)
        );
    }

}
