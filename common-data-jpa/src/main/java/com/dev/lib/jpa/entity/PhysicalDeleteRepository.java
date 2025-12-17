package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.Advised;

@RequiredArgsConstructor
public class PhysicalDeleteRepository<T extends JpaEntity> {

    private final BaseRepository<T> repository;

    private BaseRepositoryImpl<T> getImpl() {

        try {
            if (repository instanceof Advised advised) {
                return (BaseRepositoryImpl<T>) advised.getTargetSource().getTarget();
            }
            return (BaseRepositoryImpl<T>) repository;
        } catch (Exception e) {
            throw new IllegalStateException("无法获取 Repository 实现", e);
        }
    }

    public void delete(T entity) {

        getImpl().hardDelete(entity);
    }

    public void deleteById(Long id) {

        getImpl().hardDeleteById(id);
    }

    public void deleteAll(Iterable<? extends T> entities) {

        getImpl().hardDeleteAll(entities);
    }

    public void deleteAllById(Iterable<Long> ids) {

        getImpl().hardDeleteAllById(ids);
    }

    public long delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return getImpl().hardDelete(dslQuery, expressions);
    }

    public long delete(BooleanExpression... expressions) {

        return getImpl().hardDelete(null, expressions);
    }

}
