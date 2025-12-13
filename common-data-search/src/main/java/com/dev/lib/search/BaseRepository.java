package com.dev.lib.search;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.search.dsl.PredicateAssembler;
import com.dev.lib.search.dsl.SortBuilder;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public abstract class BaseRepository<T extends SearchEntity> {

    @Resource
    protected OpenSearchClient                      client;

    @Resource
    private   OpenSearchConfig.OpenSearchProperties properties;

    protected String indexName() {

        return properties.getIndex();
    }

    @SuppressWarnings("unchecked")
    protected Class<T> entityClass() {

        return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    protected Refresh refresh() {

        return Refresh.False;
    }

    // ═══════════════════════════════════════════════════════════════
    // 保存（对应 JpaRepository.save / saveAll）
    // ═══════════════════════════════════════════════════════════════
    public <S extends T> S save(S entity) {

        if (entity.isNew()) {
            prePersist(entity);
        } else {
            preUpdate(entity);
        }
        try {
            client.index(i -> i
                    .index(indexName())
                    .id(entity.getBizId())
                    .document(entity)
                    .refresh(refresh())
            );
            return entity;
        } catch (IOException e) {
            throw new RuntimeException(
                    "保存失败: " + entity.getBizId(),
                    e
            );
        }
    }

    public <S extends T> List<S> saveAll(Iterable<S> entities) {

        List<S> list = toList(entities);
        if (list.isEmpty()) return list;
        list.forEach(entity -> {
            if (entity.isNew()) {
                prePersist(entity);
            } else {
                preUpdate(entity);
            }
        });
        List<BulkOperation> operations = list.stream()
                .map(entity -> BulkOperation.of(op -> op
                        .index(idx -> idx
                                .index(indexName())
                                .id(entity.getBizId())
                                .document(entity)
                        )
                ))
                .toList();
        try {
            BulkResponse response = client.bulk(b -> b
                    .operations(operations)
                    .refresh(refresh())
            );
            if (response.errors()) {
                log.error("批量保存部分失败");
            }
            return list;
        } catch (IOException e) {
            throw new RuntimeException(
                    "批量保存失败",
                    e
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 查询（对应 JpaRepository.findById / findAll / findAllById）
    // ═══════════════════════════════════════════════════════════════
    public Optional<T> findById(String id) {

        try {
            GetResponse<T> response = client.get(
                    g -> g
                            .index(indexName())
                            .id(id),
                    entityClass()
            );
            return response.found() ? Optional.ofNullable(response.source()) : Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(
                    "查询失败: " + id,
                    e
            );
        }
    }

    public boolean existsById(String id) {

        try {
            return client.exists(e -> e
                    .index(indexName())
                    .id(id)
            ).value();
        } catch (IOException e) {
            throw new RuntimeException(
                    "判断存在失败: " + id,
                    e
            );
        }
    }

    public List<T> findAll() {

        return findAll(Sort.by(
                Sort.Direction.DESC,
                "id"
        ));
    }

    public List<T> findAll(Sort sort) {

        try {
            Set<String> allowFields = getAllowFields();
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(q -> q.matchAll(m -> m))
                            .sort(SortBuilder.build(
                                    sort,
                                    allowFields
                            )),
                    entityClass()
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(
                    "查询全部失败",
                    e
            );
        }
    }

    public Page<T> findAll(Pageable pageable) {

        try {
            Set<String> allowFields = getAllowFields();
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(q -> q.matchAll(m -> m))
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize())
                            .sort(SortBuilder.build(
                                    pageable.getSort(),
                                    allowFields
                            ))
                            .trackTotalHits(t -> t.enabled(true)),
                    entityClass()
            );
            List<T> content = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PageImpl<>(
                    content,
                    pageable,
                    total
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "分页查询失败",
                    e
            );
        }
    }

    public List<T> findAllById(Iterable<String> ids) {

        List<String> idList = toList(ids).stream()
                .map(String::valueOf)
                .toList();
        if (idList.isEmpty()) return Collections.emptyList();
        try {
            MgetResponse<T> response = client.mget(
                    m -> m
                            .index(indexName())
                            .ids(idList),
                    entityClass()
            );
            return response.docs().stream()
                    .filter(doc -> doc.result().found())
                    .map(doc -> doc.result().source())
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(
                    "批量查询失败",
                    e
            );
        }
    }

    public long count() {

        try {
            CountResponse response = client.count(c -> c
                    .index(indexName())
                    .query(q -> q.matchAll(m -> m))
            );
            return response.count();
        } catch (IOException e) {
            throw new RuntimeException(
                    "计数失败",
                    e
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 删除（对应 JpaRepository.delete / deleteById / deleteAll）
    // ═══════════════════════════════════════════════════════════════
    public void deleteById(String id) {

        try {
            client.delete(d -> d
                    .index(indexName())
                    .id(id)
                    .refresh(refresh())
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "删除失败: " + id,
                    e
            );
        }
    }

    public void delete(T entity) {

        if (entity.getBizId() != null) {
            deleteById(entity.getBizId());
        }
    }

    public void deleteAllById(Iterable<? extends String> ids) {

        List<? extends String> idList = toList(ids);
        if (idList.isEmpty()) return;
        List<BulkOperation> operations = idList.stream()
                .map(id -> BulkOperation.of(op -> op
                        .delete(d -> d.index(indexName()).id(String.valueOf(id)))
                ))
                .toList();
        try {
            client.bulk(b -> b.operations(operations).refresh(refresh()));
        } catch (IOException e) {
            throw new RuntimeException(
                    "批量删除失败",
                    e
            );
        }
    }

    public void deleteAll(Iterable<? extends T> entities) {

        List<String> ids = toList(entities).stream()
                .map(SearchEntity::getBizId)
                .filter(Objects::nonNull)
                .toList();
        if (!ids.isEmpty()) {
            deleteAllById(ids);
        }
    }

    public void deleteAll() {

        try {
            client.deleteByQuery(d -> d
                    .index(indexName())
                    .query(q -> q.matchAll(m -> m))
                    .refresh(Refresh.True)
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "删除全部失败",
                    e
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 扩展：部分更新（JPA 没有，ES 特有）
    // ═══════════════════════════════════════════════════════════════
    public void updatePartial(String id, Map<String, Object> fields) {

        if (fields == null || fields.isEmpty()) return;
        Map<String, Object> doc = new HashMap<>(fields);
        doc.put(
                "updatedAt",
                LocalDateTime.now()
        );
        doc.put(
                "modifierId",
                SecurityContextHolder.current().getId()
        );
        try {
            client.update(
                    u -> u
                            .index(indexName())
                            .id(String.valueOf(id))
                            .doc(doc)
                            .refresh(refresh()),
                    entityClass()
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "部分更新失败: " + id,
                    e
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 扩展：DSL 查询（类似 QuerydslPredicateExecutor）
    // ═══════════════════════════════════════════════════════════════
    public Optional<T> findOne(Query query) {

        try {
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(query)
                            .size(1),
                    entityClass()
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException(
                    "查询失败",
                    e
            );
        }
    }

    public List<T> findAll(Query query) {

        return findAll(
                query,
                Sort.by(
                        Sort.Direction.DESC,
                        "id"
                )
        );
    }

    public List<T> findAll(Query query, Sort sort) {

        try {
            Set<String> allowFields = getAllowFields();
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(query)
                            .sort(SortBuilder.build(
                                    sort,
                                    allowFields
                            ))
                            .size(10000),
                    entityClass()
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(
                    "查询失败",
                    e
            );
        }
    }

    public Page<T> findAll(Query query, Pageable pageable) {

        try {
            Set<String> allowFields = getAllowFields();
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(query)
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize())
                            .sort(SortBuilder.build(
                                    pageable.getSort(),
                                    allowFields
                            ))
                            .trackTotalHits(t -> t.enabled(true)),
                    entityClass()
            );
            List<T> content = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PageImpl<>(
                    content,
                    pageable,
                    total
            );
        } catch (IOException e) {
            throw new RuntimeException(
                    "分页查询失败",
                    e
            );
        }
    }

    public long count(Query query) {

        try {
            CountResponse response = client.count(c -> c
                    .index(indexName())
                    .query(query)
            );
            return response.count();
        } catch (IOException e) {
            throw new RuntimeException(
                    "计数失败",
                    e
            );
        }
    }

    public boolean exists(Query query) {

        return count(query) > 0;
    }

    public long deleteBy(Query query) {

        try {
            DeleteByQueryResponse response = client.deleteByQuery(d -> d
                    .index(indexName())
                    .query(query)
                    .refresh(Refresh.True)
            );
            return response.deleted() != null ? response.deleted() : 0;
        } catch (IOException e) {
            throw new RuntimeException(
                    "按条件删除失败",
                    e
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 扩展：DslQuery 查询（你的 DSL 框架）
    // ═══════════════════════════════════════════════════════════════
    public Optional<T> load(DslQuery<T> dslQuery, Query... extraQueries) {

        return findOne(toQuery(
                dslQuery,
                extraQueries
        ));
    }

    public List<T> loads(DslQuery<T> dslQuery, Query... extraQueries) {

        int         size        = Optional.ofNullable(dslQuery.getLimit()).orElse(100);
        int         from        = Optional.ofNullable(dslQuery.getOffset()).orElse(0);
        Set<String> allowFields = getAllowFields();
        try {
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(toQuery(
                                    dslQuery,
                                    extraQueries
                            ))
                            .from(from)
                            .size(size)
                            .sort(SortBuilder.build(
                                    dslQuery.toSort(allowFields),
                                    allowFields
                            )),
                    entityClass()
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(
                    "查询失败",
                    e
            );
        }
    }

    public Page<T> page(DslQuery<T> dslQuery, Query... extraQueries) {

        Set<String> allowFields = getAllowFields();
        Pageable    pageable    = dslQuery.toPageable(allowFields);
        return findAll(
                toQuery(
                        dslQuery,
                        extraQueries
                ),
                pageable
        );
    }

    public boolean exists(DslQuery<T> dslQuery, Query... extraQueries) {

        return exists(toQuery(
                dslQuery,
                extraQueries
        ));
    }

    public long count(DslQuery<T> dslQuery, Query... extraQueries) {

        return count(toQuery(
                dslQuery,
                extraQueries
        ));
    }

    public long deleteBy(DslQuery<T> dslQuery, Query... extraQueries) {

        return deleteBy(toQuery(
                dslQuery,
                extraQueries
        ));
    }

    // ═══════════════════════════════════════════════════════════════
    // 生命周期钩子
    // ═══════════════════════════════════════════════════════════════
    protected void prePersist(T entity) {

        LocalDateTime now  = LocalDateTime.now();
        UserDetails   user = SecurityContextHolder.current();
        if (entity.getBizId() == null) {
            entity.setBizId(IDWorker.newId());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        if (entity.getCreatorId() == null) {
            entity.setCreatorId(user.getId());
        }
        entity.setModifierId(user.getId());
        if (entity.getDeleted() == null) {
            entity.setDeleted(false);
        }
    }

    protected void preUpdate(T entity) {

        entity.setUpdatedAt(LocalDateTime.now());
        entity.setModifierId(SecurityContextHolder.current().getId());
    }

    // ═══════════════════════════════════════════════════════════════
    // 内部方法
    // ═══════════════════════════════════════════════════════════════
    private Query toQuery(DslQuery<T> dslQuery, Query... extraQueries) {

        if (dslQuery == null && extraQueries.length == 0) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        Collection<QueryFieldMerger.FieldMetaValue> fields = null;
        if (dslQuery != null) {
            List<QueryFieldMerger.FieldMetaValue>        self     = QueryFieldMerger.resolve(dslQuery);
            Map<String, QueryFieldMerger.FieldMetaValue> fieldMap = new HashMap<>();
            for (QueryFieldMerger.FieldMetaValue fv : self) {
                fieldMap.put(
                        StringUtils.format(
                                "{}-{}",
                                fv.getFieldMeta().targetField(),
                                fv.getFieldMeta().queryType()
                        ),
                        fv
                );
            }
            dslQuery.getExternalFields().forEach(it ->
                                                         fieldMap.put(
                                                                 StringUtils.format(
                                                                         "{}-{}",
                                                                         it.getFieldMeta().targetField(),
                                                                         it.getFieldMeta().queryType()
                                                                 ),
                                                                 it
                                                         ));
            fields = fieldMap.values();
        }
        return PredicateAssembler.assemble(
                dslQuery,
                fields,
                extraQueries
        );
    }

    private Set<String> getAllowFields() {

        return Arrays.stream(entityClass().getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    private <E> List<E> toList(Iterable<E> iterable) {

        if (iterable instanceof List) {
            return (List<E>) iterable;
        }
        return StreamSupport.stream(
                        iterable.spliterator(),
                        false
                )
                .collect(Collectors.toList());
    }

}
