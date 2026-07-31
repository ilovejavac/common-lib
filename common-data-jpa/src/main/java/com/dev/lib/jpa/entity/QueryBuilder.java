package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class QueryBuilder<T extends JpaEntity> {

    private final BaseRepositoryImpl<T> impl;

    private final QueryContext context = new QueryContext();

    public QueryBuilder(BaseRepositoryImpl<T> impl) {

        this.impl = impl;
    }

    public QueryBuilder<T> lockForUpdate() {

        context.lockForUpdate();
        return this;
    }

    public QueryBuilder<T> lockForShare() {

        context.lockForShare();
        return this;
    }

    public QueryBuilder<T> withDeleted() {

        context.withDeleted();
        return this;
    }

    public QueryBuilder<T> onlyDeleted() {

        context.onlyDeleted();
        return this;
    }

    public Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.load(context, dslQuery, expressions);
    }

    public List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.loads(context, dslQuery, expressions);
    }

    public Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.page(context, dslQuery, expressions);
    }

    public Stream<T> stream(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.stream(context, dslQuery, expressions);
    }

    public long count(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.count(context, dslQuery, expressions);
    }

    public long count(BooleanExpression... expressions) {

        return count(null, expressions);
    }

    public boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.exists(context, dslQuery, expressions);
    }

    public boolean exists(BooleanExpression... expressions) {

        return exists(null, expressions);
    }

    public long delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        return impl.deleteInternal(context, dslQuery, expressions);
    }

    public long delete(BooleanExpression... expressions) {

        return delete(null, expressions);
    }
}
