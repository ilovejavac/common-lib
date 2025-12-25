package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class RepositoryQuery<T extends JpaEntity> {

    private final BaseRepository<T> repository;
    private final QueryContext context;

    private BaseRepositoryImpl<T> getImpl() {

        return RepositoryUtils.unwrap(repository);
    }

    public RepositoryQuery<T> lockForUpdate() {
        context.lockForUpdate();
        return this;
    }

    public RepositoryQuery<T> lockForShare() {
        context.lockForShare();
        return this;
    }

    public RepositoryQuery<T> withDeleted() {
        context.withDeleted();
        return this;
    }

    public RepositoryQuery<T> onlyDeleted() {
        context.onlyDeleted();
        return this;
    }

    public Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return getImpl().load(context, dslQuery, expressions);
    }

    public List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return getImpl().loads(context, dslQuery, expressions);
    }

    public Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return getImpl().page(context, dslQuery, expressions);
    }

    public long count(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return getImpl().count(context, dslQuery, expressions);
    }

    public boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return getImpl().exists(context, dslQuery, expressions);
    }

    public Stream<T> stream(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        return getImpl().stream(context, dslQuery, expressions);
    }

}
