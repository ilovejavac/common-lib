package com.dev.lib.entity.dsl.agg;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.dsl.QueryRef;
import com.dev.lib.entity.dsl.QuerySetterRef;
import com.dev.lib.entity.dsl.QueryType;
import com.dev.lib.entity.dsl.core.FieldMetaCache;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public final class AggregateBuilder<E, R> {

    private static final Map<Class<?>, Map<String, FieldMetaCache.FieldMeta>> QUERY_FIELD_META_CACHE = new ConcurrentHashMap<>(128);

    private final AggregateSpec<E, R> spec;

    private final DslQuery<?> sourceQuery;

    public AggregateBuilder(AggregateSpec<E, R> spec, DslQuery<?> sourceQuery) {

        this.spec = spec;
        this.sourceQuery = sourceQuery;
    }

    @SafeVarargs
    public final AggregateBuilder<E, R> groupBy(QueryRef<E, ?>... sourceFields) {

        if (sourceFields == null) {
            return this;
        }
        for (QueryRef<E, ?> sourceField : sourceFields) {
            if (sourceField == null) {
                continue;
            }
            spec.addGroupBy(sourceField.getFieldName());
        }
        return this;
    }

    public AggregateBuilder<E, R> groupBy(String... sourceFields) {

        if (sourceFields == null) {
            return this;
        }
        for (String sourceField : sourceFields) {
            spec.addGroupBy(sourceField);
        }
        return this;
    }

    public PendingTo<E, R> field(QueryRef<E, ?> sourceField) {

        return new PendingTo<>(this, AggType.FIELD, sourceField == null ? null : sourceField.getFieldName());
    }

    public PendingTo<E, R> field(String sourceField) {

        return new PendingTo<>(this, AggType.FIELD, sourceField);
    }

    public PendingTo<E, R> count(QueryRef<E, ?> sourceField) {

        return new PendingTo<>(this, AggType.COUNT, sourceField == null ? null : sourceField.getFieldName());
    }

    public PendingTo<E, R> count(String sourceField) {

        return new PendingTo<>(this, AggType.COUNT, sourceField);
    }

    public PendingTo<E, R> countDistinct(QueryRef<E, ?> sourceField) {

        return new PendingTo<>(this, AggType.COUNT_DISTINCT, sourceField == null ? null : sourceField.getFieldName());
    }

    public PendingTo<E, R> countDistinct(String sourceField) {

        return new PendingTo<>(this, AggType.COUNT_DISTINCT, sourceField);
    }

    public <N extends Number> PendingTo<E, R> sum(QueryRef<E, N> sourceField) {

        return new PendingTo<>(this, AggType.SUM, sourceField == null ? null : sourceField.getFieldName());
    }

    public PendingTo<E, R> sum(String sourceField) {

        return new PendingTo<>(this, AggType.SUM, sourceField);
    }

    public PendingTo<E, R> min(QueryRef<E, ?> sourceField) {

        return new PendingTo<>(this, AggType.MIN, sourceField == null ? null : sourceField.getFieldName());
    }

    public PendingTo<E, R> min(String sourceField) {

        return new PendingTo<>(this, AggType.MIN, sourceField);
    }

    public PendingTo<E, R> max(QueryRef<E, ?> sourceField) {

        return new PendingTo<>(this, AggType.MAX, sourceField == null ? null : sourceField.getFieldName());
    }

    public PendingTo<E, R> max(String sourceField) {

        return new PendingTo<>(this, AggType.MAX, sourceField);
    }

    public <N extends Number> PendingTo<E, R> avg(QueryRef<E, N> sourceField) {

        return new PendingTo<>(this, AggType.AVG, sourceField == null ? null : sourceField.getFieldName());
    }

    public PendingTo<E, R> avg(String sourceField) {

        return new PendingTo<>(this, AggType.AVG, sourceField);
    }

    public <Q> AggregateBuilder<E, R> having(QueryRef<Q, ?> queryFieldRef) {

        if (queryFieldRef == null) {
            throw new IllegalArgumentException("having 字段不能为空");
        }
        if (sourceQuery == null) {
            throw new IllegalStateException("having 需要绑定 DslQuery 实例");
        }

        String queryFieldName = queryFieldRef.getFieldName();
        FieldMetaCache.FieldMeta fieldMeta = resolveQueryFieldMeta(queryFieldName);
        if (fieldMeta == null) {
            throw new IllegalArgumentException("having 字段不存在: " + queryFieldName);
        }
        if (!fieldMeta.isCondition()) {
            throw new IllegalArgumentException("having 仅支持 Condition 字段: " + queryFieldName);
        }

        QueryType queryType = fieldMeta.queryType() == null ? QueryType.EQ : fieldMeta.queryType();
        Object value = fieldMeta.getValue(sourceQuery);
        if (value == null && queryType != QueryType.IS_NULL && queryType != QueryType.IS_NOT_NULL) {
            return this;
        }

        spec.addHaving(fieldMeta.targetField(), queryType, value);
        clearQueryFieldValue(fieldMeta.field());
        return this;
    }

    @SafeVarargs
    public final AggregateBuilder<E, R> orderByAsc(QueryRef<R, ?>... targetFields) {

        return orderBy(true, targetFields);
    }

    @SafeVarargs
    public final AggregateBuilder<E, R> orderByDesc(QueryRef<R, ?>... targetFields) {

        return orderBy(false, targetFields);
    }

    public AggregateBuilder<E, R> offset(Integer offset) {

        spec.setOffset(offset);
        return this;
    }

    public AggregateBuilder<E, R> limit(Integer limit) {

        spec.setLimit(limit);
        return this;
    }

    public AggregateBuilder<E, R> page(Integer offset, Integer limit) {

        spec.setOffset(offset);
        spec.setLimit(limit);
        return this;
    }

    public AggregateBuilder<E, R> joinAuto() {

        spec.setJoinStrategy(AggJoinStrategy.AUTO);
        return this;
    }

    public AggregateBuilder<E, R> joinInner() {

        spec.setJoinStrategy(AggJoinStrategy.INNER);
        return this;
    }

    public AggregateBuilder<E, R> joinLeft() {

        spec.setJoinStrategy(AggJoinStrategy.LEFT);
        return this;
    }

    public AggregateSpec<E, R> spec() {

        return spec;
    }

    private FieldMetaCache.FieldMeta resolveQueryFieldMeta(String queryFieldName) {

        Map<String, FieldMetaCache.FieldMeta> metaByField = QUERY_FIELD_META_CACHE.computeIfAbsent(sourceQuery.getClass(), queryClass -> {
            Map<String, FieldMetaCache.FieldMeta> metaMap = new LinkedHashMap<>();
            for (FieldMetaCache.FieldMeta fieldMeta : FieldMetaCache.resolveFieldMeta(queryClass)) {
                metaMap.putIfAbsent(fieldMeta.field().getName(), fieldMeta);
            }
            return metaMap;
        });
        return metaByField.get(queryFieldName);
    }

    private void clearQueryFieldValue(Field field) {

        if (field == null || field.getType().isPrimitive()) {
            return;
        }
        try {
            field.set(sourceQuery, null);
        } catch (Exception ignored) {
        }
    }

    @SafeVarargs
    private final AggregateBuilder<E, R> orderBy(boolean asc, QueryRef<R, ?>... targetFields) {

        if (targetFields == null) {
            return this;
        }
        for (QueryRef<R, ?> targetField : targetFields) {
            if (targetField == null) {
                continue;
            }
            spec.addOrder(targetField.getFieldName(), asc);
        }
        return this;
    }

    public static final class PendingTo<E, R> {

        private final AggregateBuilder<E, R> parent;

        private final AggType type;

        private final String sourceField;

        private PendingTo(AggregateBuilder<E, R> parent, AggType type, String sourceField) {

            this.parent = parent;
            this.type = type;
            this.sourceField = sourceField;
        }

        public AggregateBuilder<E, R> to(QueryRef<R, ?> targetField) {

            if (sourceField == null || sourceField.isBlank()) {
                throw new IllegalArgumentException("聚合源字段不能为空");
            }
            if (targetField == null) {
                throw new IllegalArgumentException("聚合目标字段不能为空");
            }
            parent.spec.addItem(type, sourceField, targetField.getFieldName());
            return parent;
        }

        public <V> AggregateBuilder<E, R> to(QuerySetterRef<R, V> targetSetter) {

            if (sourceField == null || sourceField.isBlank()) {
                throw new IllegalArgumentException("聚合源字段不能为空");
            }
            if (targetSetter == null) {
                throw new IllegalArgumentException("聚合目标字段不能为空");
            }
            parent.spec.addItem(type, sourceField, targetSetter.getFieldName());
            return parent;
        }
    }
}
