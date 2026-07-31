package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.delete.PhysicalDeleteSupport;
import com.querydsl.core.types.dsl.BooleanExpression;

public final class PhysicalDeleteRepository<T extends JpaEntity> {

    private final BaseRepositoryImpl<T> impl;

    PhysicalDeleteRepository(BaseRepositoryImpl<T> impl) {

        this.impl = impl;
    }

    public void delete(T entity) {

        RepositoryTransactionSupport.run(impl, () -> PhysicalDeleteSupport.deleteEntity(impl, entity));
    }

    public void deleteById(Long id) {

        RepositoryTransactionSupport.run(impl, () -> PhysicalDeleteSupport.deleteById(impl, id));
    }

    public void deleteAll(Iterable<? extends T> entities) {

        RepositoryTransactionSupport.run(impl, () -> PhysicalDeleteSupport.deleteAll(impl, entities));
    }

    public void deleteAllById(Iterable<Long> ids) {

        RepositoryTransactionSupport.run(impl, () -> PhysicalDeleteSupport.deleteAllById(impl, ids));
    }

    public long delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return RepositoryTransactionSupport.call(
                impl,
                () -> PhysicalDeleteSupport.delete(impl, dslQuery, expressions)
        );
    }

    public long delete(BooleanExpression... expressions) {

        return delete(null, expressions);
    }
}
