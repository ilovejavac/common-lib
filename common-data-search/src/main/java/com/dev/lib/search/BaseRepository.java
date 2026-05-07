package com.dev.lib.search;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.core.DslQueryFieldResolver;
import com.dev.lib.entity.dsl.core.QueryFieldMerger;
import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.search.dsl.PredicateAssembler;
import com.dev.lib.search.dsl.SortBuilder;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@SuppressWarnings("all")
public abstract class BaseRepository<T extends SearchEntity> implements SearchRepository<T> {

    private static final int BATCH_SIZE = 256;

    private static final int DEFAULT_PAGE_SIZE = 128;

    private static final int DEFAULT_LOADS_SIZE = 10000;

    @Resource
    protected OpenSearchClient client;

    @Autowired(required = false)
    private OpenSearchConfig.OpenSearchProperties properties;

    protected String indexName() {

        SearchIndex searchIndex = entityClass().getAnnotation(SearchIndex.class);
        if (searchIndex != null && !searchIndex.value().isBlank()) {
            return searchIndex.value();
        }
        if (properties != null && properties.getIndex() != null && !properties.getIndex().isBlank()) {
            return properties.getIndex();
        }
        return entityClass().getSimpleName().toLowerCase(Locale.ROOT);
    }

    private volatile Class<T> entityClassCache;

    @SuppressWarnings("unchecked")
    protected Class<T> entityClass() {

        if (entityClassCache == null) {
            entityClassCache = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0];
        }
        return entityClassCache;
    }

    protected Refresh refresh() {

        return Refresh.False;
    }

    // ═══════════════════════════════════════════════════════════════
    // 保存
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
            throw new RuntimeException("保存失败: " + entity.getBizId(), e);
        }
    }

    public <S extends T> List<S> saveAll(Iterable<S> entities) {

        List<S> list = toList(entities);
        if (list.isEmpty()) return list;

        List<BulkOperation> batch = new ArrayList<>(BATCH_SIZE + 1);
        for (S entity : list) {
            if (entity.isNew()) {
                prePersist(entity);
            } else {
                preUpdate(entity);
            }
            batch.add(BulkOperation.of(op -> op
                    .index(idx -> idx
                            .index(indexName())
                            .id(entity.getBizId())
                            .document(entity)
                    )
            ));
            if (batch.size() >= BATCH_SIZE) {
                executeBulk(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            executeBulk(batch);
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    // 查询
    // ═══════════════════════════════════════════════════════════════
    public Optional<T> findById(String id) {

        try {
            GetResponse<T> response = client.get(
                    g -> g.index(indexName()).id(id),
                    entityClass()
            );
            return response.found() ? Optional.ofNullable(response.source()) : Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("查询失败: " + id, e);
        }
    }

    public boolean existsById(String id) {

        try {
            return client.exists(e -> e.index(indexName()).id(id)).value();
        } catch (IOException e) {
            throw new RuntimeException("判断存在失败: " + id, e);
        }
    }

    public List<T> findAll() {

        return findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<T> findAll(Sort sort) {

        try {
            Map<String, Class<?>> fieldTypes = getFieldTypes();
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(q -> q.matchAll(m -> m))
                            .size(DEFAULT_LOADS_SIZE)
                            .sort(SortBuilder.build(sort, fieldTypes)),
                    entityClass()
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("查询全部失败", e);
        }
    }

    public Page<T> findAll(Pageable pageable) {

        try {
            Map<String, Class<?>> fieldTypes = getFieldTypes();
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(q -> q.matchAll(m -> m))
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize())
                            .sort(SortBuilder.build(pageable.getSort(), fieldTypes))
                            .trackTotalHits(t -> t.enabled(true)),
                    entityClass()
            );
            List<T> content = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PageImpl<>(content, pageable, total);
        } catch (IOException e) {
            throw new RuntimeException("分页查询失败", e);
        }
    }

    public List<T> findAllById(Iterable<String> ids) {

        if (ids == null) {
            return Collections.emptyList();
        }
        List<String> idList = toList(ids).stream()
                .map(String::valueOf)
                .toList();
        if (idList.isEmpty()) return Collections.emptyList();
        try {
            MgetResponse<T> response = client.mget(
                    m -> m.index(indexName()).ids(idList),
                    entityClass()
            );
            return response.docs().stream()
                    .filter(doc -> doc.result().found())
                    .map(doc -> doc.result().source())
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("批量查询失败", e);
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
            throw new RuntimeException("计数失败", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 删除
    // ═══════════════════════════════════════════════════════════════
    public void deleteById(String id) {

        try {
            client.delete(d -> d
                    .index(indexName())
                    .id(id)
                    .refresh(refresh())
            );
        } catch (IOException e) {
            throw new RuntimeException("删除失败: " + id, e);
        }
    }

    public void delete(T entity) {

        if (entity.getBizId() != null) {
            deleteById(entity.getBizId());
        }
    }

    public void deleteAllById(Iterable<? extends String> ids) {

        if (ids == null) {
            return;
        }
        List<BulkOperation> batch = new ArrayList<>(BATCH_SIZE + 1);
        for (String id : ids) {
            if (id == null) continue;
            batch.add(BulkOperation.of(op -> op
                    .delete(d -> d.index(indexName()).id(id))
            ));
            if (batch.size() >= BATCH_SIZE) {
                executeBulk(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            executeBulk(batch);
        }
    }

    public void deleteAll(Iterable<? extends T> entities) {

        if (entities == null) {
            return;
        }
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
            throw new RuntimeException("删除全部失败", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 部分更新
    // ═══════════════════════════════════════════════════════════════
    public void updatePartial(String id, Map<String, Object> fields) {

        if (fields == null || fields.isEmpty()) return;
        Map<String, Object> doc = new HashMap<>(fields);
        doc.put("updatedAt", LocalDateTime.now());
        doc.put("modifierId", SecurityContextHolder.current().getId());
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
            throw new RuntimeException("部分更新失败: " + id, e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DSL 查询
    // ═══════════════════════════════════════════════════════════════
    public Optional<T> findOne(Query query) {

        try {
            SearchResponse<T> response = client.search(
                    s -> s.index(indexName()).query(query).size(1),
                    entityClass()
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .findFirst();
        } catch (IOException e) {
            throw new RuntimeException("查询失败", e);
        }
    }

    public List<T> findAll(Query query) {

        return findAll(query, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<T> findAll(Query query, Sort sort) {

        try {
            Map<String, Class<?>> fieldTypes = getFieldTypes();
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(query)
                            .sort(SortBuilder.build(sort, fieldTypes))
                            .size(10000),
                    entityClass()
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("查询失败", e);
        }
    }

    public Page<T> findAll(Query query, Pageable pageable) {

        try {
            Map<String, Class<?>> fieldTypes = getFieldTypes();
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(query)
                            .from((int) pageable.getOffset())
                            .size(pageable.getPageSize())
                            .sort(SortBuilder.build(pageable.getSort(), fieldTypes))
                            .trackTotalHits(t -> t.enabled(true)),
                    entityClass()
            );
            List<T> content = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
            long total = response.hits().total() != null ? response.hits().total().value() : 0;
            return new PageImpl<>(content, pageable, total);
        } catch (IOException e) {
            throw new RuntimeException("分页查询失败", e);
        }
    }

    public long count(Query query) {

        try {
            CountResponse response = client.count(c -> c.index(indexName()).query(query));
            return response.count();
        } catch (IOException e) {
            throw new RuntimeException("计数失败", e);
        }
    }

    public boolean exists(Query query) {

        return count(query) > 0;
    }

    public long delete(Query query) {

        try {
            DeleteByQueryResponse response = client.deleteByQuery(d -> d
                    .index(indexName())
                    .query(query)
                    .refresh(Refresh.True)
            );
            return response.deleted() != null ? response.deleted() : 0;
        } catch (IOException e) {
            throw new RuntimeException("按条件删除失败", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DslQuery 查询
    // ═══════════════════════════════════════════════════════════════
    public Optional<T> load(DslQuery<T> dslQuery, Query... extraQueries) {

        ensureNonAggregateQuery(dslQuery, "load");
        return findOne(toQuery(dslQuery, extraQueries));
    }

    public List<T> loads(DslQuery<T> dslQuery, Query... extraQueries) {

        ensureNonAggregateQuery(dslQuery, "loads");
        int                   size       = dslQuery != null ? Optional.ofNullable(dslQuery.getLimit()).orElse(DEFAULT_LOADS_SIZE) : DEFAULT_LOADS_SIZE;
        int                   from       = dslQuery != null ? Optional.ofNullable(dslQuery.getOffset()).orElse(0) : 0;
        Map<String, Class<?>> fieldTypes = getFieldTypes();
        Sort                  sort       = dslQuery != null ? dslQuery.toSort(fieldTypes.keySet()) : Sort.by(Sort.Order.desc("id"));
        try {
            SearchResponse<T> response = client.search(
                    s -> s
                            .index(indexName())
                            .query(toQuery(dslQuery, extraQueries))
                            .from(from)
                            .size(size)
                            .sort(SortBuilder.build(sort, fieldTypes)),
                    entityClass()
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("查询失败", e);
        }
    }

    public Page<T> page(DslQuery<T> dslQuery, Query... extraQueries) {

        ensureNonAggregateQuery(dslQuery, "page");
        Map<String, Class<?>> fieldTypes = getFieldTypes();
        Pageable              pageable   = resolvePageable(dslQuery, fieldTypes.keySet());
        return findAll(toQuery(dslQuery, extraQueries), pageable);
    }

    public boolean exists(DslQuery<T> dslQuery, Query... extraQueries) {

        ensureNonAggregateQuery(dslQuery, "exists");
        return exists(toQuery(dslQuery, extraQueries));
    }

    public long count(DslQuery<T> dslQuery, Query... extraQueries) {

        ensureNonAggregateQuery(dslQuery, "count");
        return count(toQuery(dslQuery, extraQueries));
    }

    public long delete(DslQuery<T> dslQuery, Query... extraQueries) {

        ensureNonAggregateQuery(dslQuery, "delete");
        if (!hasBusinessCondition(dslQuery, extraQueries)) {
            throw new IllegalArgumentException("批量删除必须指定业务条件，防止误删全表");
        }
        return delete(toQuery(dslQuery, extraQueries));
    }

    // ═══════════════════════════════════════════════════════════════
    // 生命周期钩子
    // ═══════════════════════════════════════════════════════════════
    protected void prePersist(T entity) {

        LocalDateTime now  = LocalDateTime.now();
        UserDetails   user = SecurityContextHolder.current();

        if (entity.getId() == null) {
            entity.setId(IDWorker.nextID());  // 加这个
        }
        if (entity.getBizId() == null) {
            entity.setBizId(IDWorker.newId());
        }
        entity.setDeletedAt(null);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        if (entity.getCreatorId() == null) {
            entity.setCreatorId(user.getId());
        }
        entity.setModifierId(user.getId());
    }

    protected void preUpdate(T entity) {

        entity.setUpdatedAt(LocalDateTime.now());
        entity.setModifierId(SecurityContextHolder.current().getId());
    }

    // ═══════════════════════════════════════════════════════════════
    // 内部方法
    // ═══════════════════════════════════════════════════════════════
    private Query toQuery(DslQuery<T> dslQuery, Query... extraQueries) {

        Query[] normalizedExtraQueries = extraQueries == null ? new Query[0] : extraQueries;
        if (dslQuery == null && normalizedExtraQueries.length == 0) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        Collection<QueryFieldMerger.FieldMetaValue> fields = null;
        if (dslQuery != null) {
            fields = DslQueryFieldResolver.resolveMerged(
                    dslQuery,
                    DslQueryFieldResolver.OverridePolicy.EXTERNAL_OVERRIDE_SELF
            );
        }
        return PredicateAssembler.assemble(dslQuery, fields, normalizedExtraQueries);
    }

    private Pageable resolvePageable(DslQuery<T> dslQuery, Set<String> allowedFields) {

        if (dslQuery == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, Sort.by(Sort.Order.desc("id")));
        }
        return dslQuery.toPageable(allowedFields);
    }

    private boolean hasBusinessCondition(DslQuery<T> dslQuery, Query... extraQueries) {

        if (extraQueries != null && Arrays.stream(extraQueries).anyMatch(Objects::nonNull)) {
            return true;
        }
        if (dslQuery == null) {
            return false;
        }
        return !DslQueryFieldResolver.resolveMerged(
                dslQuery,
                DslQueryFieldResolver.OverridePolicy.EXTERNAL_OVERRIDE_SELF
        ).isEmpty();
    }

    private void ensureNonAggregateQuery(DslQuery<T> query, String operation) {

        if (query != null && query.hasAgg()) {
            throw new IllegalStateException("检测到 agg() 聚合配置，" + operation + " 不支持聚合查询");
        }
    }

    private static final Map<Class<?>, Map<String, Class<?>>> FIELD_TYPE_CACHE = new ConcurrentHashMap<>();

    private Map<String, Class<?>> getFieldTypes() {

        return FIELD_TYPE_CACHE.computeIfAbsent(
                entityClass(), BaseRepository::scanFieldTypes
        );
    }

    private static Map<String, Class<?>> scanFieldTypes(Class<?> clazz) {

        Map<String, Class<?>> types = new LinkedHashMap<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Arrays.stream(current.getDeclaredFields())
                    .forEach(field -> types.putIfAbsent(field.getName(), field.getType()));
            current = current.getSuperclass();
        }
        return types;
    }

    private void executeBulk(List<BulkOperation> operations) {

        try {
            BulkResponse response = client.bulk(b -> b
                    .operations(operations)
                    .refresh(refresh())
            );
            if (response.errors()) {
                String errorMessage = response.items().stream()
                        .filter(item -> item.error() != null)
                        .findFirst()
                        .map(item -> "批量操作部分失败: id=" + item.id()
                                     + ", type=" + item.error().type()
                                     + ", reason=" + item.error().reason())
                        .orElse("批量操作部分失败");
                throw new RuntimeException(errorMessage);
            }
        } catch (IOException e) {
            throw new RuntimeException("批量操作失败", e);
        }
    }

    private <E> List<E> toList(Iterable<E> iterable) {

        if (iterable == null) {
            return Collections.emptyList();
        }
        if (iterable instanceof List) {
            return (List<E>) iterable;
        }
        return StreamSupport.stream(iterable.spliterator(), false).toList();
    }

}
