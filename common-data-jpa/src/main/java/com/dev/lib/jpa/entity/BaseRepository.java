package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@NoRepositoryBean
public interface BaseRepository<T extends JpaEntity> extends JpaRepository<T, Long>, ListQuerydslPredicateExecutor<T> {

    // 查询修饰符
    default RepositoryQuery<T> lockForUpdate() {

        return new RepositoryQuery<>(this, new QueryContext().lockForUpdate());
    }

    default RepositoryQuery<T> lockForShare() {

        return new RepositoryQuery<>(this, new QueryContext().lockForShare());
    }

    default RepositoryQuery<T> withDeleted() {

        return new RepositoryQuery<>(this, new QueryContext().withDeleted());
    }

    default RepositoryQuery<T> onlyDeleted() {

        return new RepositoryQuery<>(this, new QueryContext().onlyDeleted());
    }

    default PhysicalDeleteRepository<T> physicalDelete() {

        return new PhysicalDeleteRepository<>(this);
    }

    default void delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {
        deleteAll(loads(dslQuery, expressions));
    }

    // 查询
    Stream<T> stream(DslQuery<T> dslQuery, BooleanExpression... expressions);

    default Stream<T> stream() {
        return stream(null);
    }

    Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions);

    List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions);

    Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions);

    boolean exists(DslQuery<T> dslQuery, BooleanExpression... expressions);

    long count(DslQuery<T> dslQuery, BooleanExpression... expressions);

}

