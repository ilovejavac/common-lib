package com.dev.lib.search;

import com.dev.lib.entity.dsl.DslQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SearchRepository<T extends SearchEntity> {

    // 保存
    <S extends T> S save(S entity);
    <S extends T> List<S> saveAll(Iterable<S> entities);

    // 基础查询
    Optional<T> findById(String id);
    boolean existsById(String id);
    List<T> findAll();
    List<T> findAll(Sort sort);
    Page<T> findAll(Pageable pageable);
    List<T> findAllById(Iterable<String> ids);
    long count();

    // 删除
    void deleteById(String id);
    void delete(T entity);
    void deleteAllById(Iterable<? extends String> ids);
    void deleteAll(Iterable<? extends T> entities);
    void deleteAll();

    // 部分更新
    void updatePartial(String id, Map<String, Object> fields);

    // Query DSL
    Optional<T> findOne(Query query);
    List<T> findAll(Query query);
    List<T> findAll(Query query, Sort sort);
    Page<T> findAll(Query query, Pageable pageable);
    long count(Query query);
    boolean exists(Query query);
    long delete(Query query);

    // DslQuery
    Optional<T> load(DslQuery<T> dslQuery, Query... extraQueries);
    List<T> loads(DslQuery<T> dslQuery, Query... extraQueries);
    Page<T> page(DslQuery<T> dslQuery, Query... extraQueries);
    boolean exists(DslQuery<T> dslQuery, Query... extraQueries);
    long count(DslQuery<T> dslQuery, Query... extraQueries);
    long delete(DslQuery<T> dslQuery, Query... extraQueries);
}
