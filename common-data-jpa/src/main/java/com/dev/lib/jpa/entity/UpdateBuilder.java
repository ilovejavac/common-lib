package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.encrypt.Encrypt;
import com.dev.lib.jpa.TransactionHelper;
import com.dev.lib.jpa.entity.dsl.SFunction;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.util.encrypt.EncryptUtil;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.Column;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UpdateBuilder<T extends JpaEntity> {

    private static final String ID_FIELD_NAME = "id";

    private static final String BIZ_ID_FIELD_NAME = "bizId";

    private static final Map<Class<?>, Map<String, FieldMeta>> FIELD_META_CACHE = new ConcurrentHashMap<>();

    private final BaseRepositoryImpl<T> impl;

    private final Map<String, Assignment> assignments = new LinkedHashMap<>();

    private DslQuery<T> dslQuery;

    private BooleanExpression[] expressions = new BooleanExpression[0];

    private Predicate predicate;

    private Predicate businessPredicate;

    private BooleanExpression identityExpression;

    private boolean idIdentityPresent;

    public UpdateBuilder(BaseRepositoryImpl<T> impl) {

        this.impl = impl;
    }

    public UpdateBuilder<T> set(SFunction<T, ?> field, Object value) {

        if (value == null) {
            return this;
        }

        FieldMeta meta = resolveFieldMeta(field);
        if (applyIdentityCondition(meta, value)) {
            return this;
        }
        assignments.put(meta.fieldName(), new Assignment(meta, prepareAssignmentValue(meta, value), false));
        return this;
    }

    public UpdateBuilder<T> setNull(SFunction<T, ?> field) {

        FieldMeta meta = resolveFieldMeta(field);
        if (!meta.nullable()) {
            throw new IllegalArgumentException("字段不允许置空(nullable = false): " + meta.fieldName());
        }
        assignments.put(meta.fieldName(), new Assignment(meta, null, true));
        return this;
    }

    public UpdateBuilder<T> where(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        this.dslQuery = dslQuery;
        this.expressions = expressions == null ? new BooleanExpression[0] : expressions;
        return this;
    }

    public long execute() {

        refreshPredicates();

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("至少设置一个字段");
        }
        if (RepositoryPredicateSupport.isEmptyPredicate(businessPredicate)) {
            throw new IllegalArgumentException("批量更新必须指定业务条件，防止误更新全表");
        }

        ensureAuditAssignments();

        return TransactionHelper.callWithEntityManagerFactory(impl.getEntityManagerFactory(), () -> {
            JPAUpdateClause clause = impl.getQueryFactory().update(impl.getPath());
            for (Assignment assignment : assignments.values()) {
                applyAssignment(clause, assignment);
            }

            long affected = clause.where(predicate).execute();
            if (affected > 0) {
                impl.getEntityManager().flush();
                impl.getEntityManager().clear();
            }
            return affected;
        });
    }

    private boolean applyIdentityCondition(FieldMeta meta, Object value) {

        if (ID_FIELD_NAME.equals(meta.fieldName())) {
            if (!(value instanceof Long id)) {
                throw new IllegalArgumentException("id 条件值必须是 Long: " + value.getClass().getName());
            }
            identityExpression = impl.getIdPath().eq(id);
            idIdentityPresent = true;
            return true;
        }

        if (BIZ_ID_FIELD_NAME.equals(meta.fieldName())) {
            if (idIdentityPresent) {
                return true;
            }
            if (!(value instanceof String bizId)) {
                throw new IllegalArgumentException("bizId 条件值必须是 String: " + value.getClass().getName());
            }
            identityExpression = impl.getPathBuilder().getString(BIZ_ID_FIELD_NAME).eq(bizId);
            return true;
        }
        return false;
    }

    private Object prepareAssignmentValue(FieldMeta meta, Object value) {

        if (!meta.encrypt()) {
            return value;
        }
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException("@Encrypt 字段只支持 String 类型: " + meta.fieldName());
        }
        return EncryptUtil.encrypt(stringValue);
    }

    private void refreshPredicates() {

        BooleanExpression[] mergedExpressions = mergeIdentityExpression();
        this.businessPredicate = RepositoryPredicateSupport.toPredicate(dslQuery, mergedExpressions);
        this.predicate = RepositoryPredicateSupport.buildPredicate(
                impl.getPathBuilder(),
                impl.getPath(),
                impl.getDeletedPath(),
                new QueryContext(),
                dslQuery,
                mergedExpressions
        );
    }

    private BooleanExpression[] mergeIdentityExpression() {

        if (identityExpression == null) {
            return expressions;
        }

        BooleanExpression[] merged = Arrays.copyOf(expressions, expressions.length + 1);
        merged[expressions.length] = identityExpression;
        return merged;
    }

    private void ensureAuditAssignments() {

        putAuditIfAbsent("updatedAt", LocalDateTime.now());
        putAuditIfAbsent("modifierId", SecurityContextHolder.getUserId());
    }

    private void putAuditIfAbsent(String fieldName, Object value) {

        if (value == null || assignments.containsKey(fieldName)) {
            return;
        }

        FieldMeta meta = resolveFieldMetaMap().get(fieldName);
        if (meta == null) {
            return;
        }
        assignments.put(fieldName, new Assignment(meta, value, false));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applyAssignment(JPAUpdateClause clause, Assignment assignment) {

        Path path = impl.getPathBuilder().get(assignment.meta().fieldName(), assignment.meta().fieldType());
        if (assignment.isNull()) {
            clause.setNull(path);
            return;
        }
        clause.set(path, assignment.value());
    }

    private FieldMeta resolveFieldMeta(SFunction<T, ?> field) {

        return resolveFieldMeta(field.getFieldName());
    }

    private FieldMeta resolveFieldMeta(String fieldName) {

        FieldMeta meta = resolveFieldMetaMap().get(fieldName);
        if (meta == null) {
            throw new IllegalArgumentException("字段不存在: " + fieldName);
        }
        return meta;
    }

    private Map<String, FieldMeta> resolveFieldMetaMap() {

        return FIELD_META_CACHE.computeIfAbsent(impl.getEntityClass(), UpdateBuilder::scanFieldMeta);
    }

    private static Map<String, FieldMeta> scanFieldMeta(Class<?> entityClass) {

        Map<String, FieldMeta> map = new ConcurrentHashMap<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                ReflectionUtils.makeAccessible(field);
                Column column = field.getAnnotation(Column.class);
                boolean nullable = column == null ? !field.getType().isPrimitive() : column.nullable();
                map.putIfAbsent(field.getName(), new FieldMeta(
                        field.getName(),
                        field.getType(),
                        nullable,
                        field.isAnnotationPresent(Encrypt.class)
                ));
            }
            current = current.getSuperclass();
        }
        return map;
    }

    private record FieldMeta(String fieldName, Class<?> fieldType, boolean nullable, boolean encrypt) {
    }

    private record Assignment(FieldMeta meta, Object value, boolean isNull) {
    }
}
