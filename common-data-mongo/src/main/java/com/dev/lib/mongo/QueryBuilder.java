package com.dev.lib.mongo;

import com.dev.lib.entity.dsl.DslQuery;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public class QueryBuilder<T extends MongoEntity> {

    private final BaseRepository<T> repository;

    private DeletedFilter deletedFilter = DeletedFilter.EXCLUDE_DELETED;

    public QueryBuilder(BaseRepository<T> repository) {

        this.repository = repository;
    }

    public QueryBuilder<T> withDeleted() {

        this.deletedFilter = DeletedFilter.INCLUDE_DELETED;
        return this;
    }

    public QueryBuilder<T> onlyDeleted() {

        this.deletedFilter = DeletedFilter.ONLY_DELETED;
        return this;
    }

    public Optional<T> load(DslQuery<T> query, BooleanExpression... expressions) {

        return repository.load(deletedFilter, query, expressions);
    }

    public List<T> loads(DslQuery<T> query, BooleanExpression... expressions) {

        return repository.loads(deletedFilter, query, expressions);
    }

    public Page<T> page(DslQuery<T> query, BooleanExpression... expressions) {

        return repository.page(deletedFilter, query, expressions);
    }

    public boolean exists(DslQuery<T> query, BooleanExpression... expressions) {

        return repository.exists(deletedFilter, query, expressions);
    }

    public long count(DslQuery<T> query, BooleanExpression... expressions) {

        return repository.count(deletedFilter, query, expressions);
    }

    public long delete(DslQuery<T> query, BooleanExpression... expressions) {

        return repository.delete(deletedFilter, query, expressions);
    }
}
