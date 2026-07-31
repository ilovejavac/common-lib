package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.entity.encrypt.Encrypt;
import com.dev.lib.jpa.entity.dsl.SFunction;
import com.dev.lib.jpa.entity.query.RepositoryPredicateSupport;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.util.encrypt.EncryptUtil;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAUpdateClause;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.util.ReflectionUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 QueryDSL 的批量更新，所有赋值在数据库侧一次 UPDATE 完成，避免先查后改的并发竞争。
 * 调用方需要处于事务中（例如方法标注 @Transactional）。
 */
public final class UpdateBuilder<T extends JpaEntity> {

    private static final Map<Class<?>, Map<String, FieldMeta>> FIELD_META_CACHE = new ConcurrentHashMap<>();

    private final BaseRepositoryImpl<T> impl;

    private final Map<String, Assignment> assignments = new LinkedHashMap<>();

    private DslQuery<T> dslQuery;

    private BooleanExpression[] expressions = new BooleanExpression[0];

    UpdateBuilder(BaseRepositoryImpl<T> impl) {

        this.impl = impl;
    }

    public UpdateBuilder<T> set(SFunction<T, ?> field, Object value) {

        if (value == null) {
            return this;
        }
        FieldMeta meta = resolveFieldMeta(field.getFieldName());
        assignments.put(meta.fieldName(), new Assignment(meta, prepareValue(meta, value)));
        return this;
    }

    public UpdateBuilder<T> where(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        this.dslQuery = dslQuery;
        this.expressions = expressions == null ? new BooleanExpression[0] : expressions;
        return this;
    }

    public long execute() {

        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("至少设置一个字段");
        }
        Predicate businessPredicate = RepositoryPredicateSupport.toPredicate(dslQuery, expressions);
        if (RepositoryPredicateSupport.isEmptyPredicate(businessPredicate)) {
            throw new IllegalArgumentException("批量更新必须指定业务条件，防止误更新全表");
        }

        ensureAuditAssignments();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return doExecute();
        }
        // 无活动事务时自管理一个事务，保证批量更新的原子性（行为与旧版 TransactionHelper 一致）
        TransactionTemplate template = new TransactionTemplate(new JpaTransactionManager(impl.getEntityManagerFactory()));
        return template.execute(status -> doExecute());
    }

    private long doExecute() {

        JPAUpdateClause clause = impl.getQueryFactory().update(impl.getPath());
        for (Assignment assignment : assignments.values()) {
            applyAssignment(clause, assignment);
        }
        Predicate predicate = RepositoryPredicateSupport.buildPredicate(
                impl.getPathBuilder(),
                impl.getPath(),
                impl.getDeletedPath(),
                dslQuery,
                expressions
        );
        long affected = clause.where(predicate).execute();
        if (affected > 0) {
            impl.getEntityManager().flush();
            impl.getEntityManager().clear();
        }
        return affected;
    }

    private void ensureAuditAssignments() {

        putAuditIfAbsent("updatedAt", LocalDateTime.now());
        putAuditIfAbsent("modifierId", SecurityContextHolder.getUserId());
    }

    private void putAuditIfAbsent(String fieldName, Object value) {

        if (value == null || assignments.containsKey(fieldName)) {
            return;
        }
        FieldMeta meta = fieldMetaMap().get(fieldName);
        if (meta != null) {
            assignments.put(fieldName, new Assignment(meta, value));
        }
    }

    private Object prepareValue(FieldMeta meta, Object value) {

        if (!meta.encrypt()) {
            return value;
        }
        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException("@Encrypt 字段只支持 String 类型: " + meta.fieldName());
        }
        return EncryptUtil.encrypt(stringValue);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void applyAssignment(JPAUpdateClause clause, Assignment assignment) {

        Path path = impl.getPathBuilder().get(assignment.meta().fieldName(), assignment.meta().fieldType());
        clause.set(path, assignment.value());
    }

    private FieldMeta resolveFieldMeta(String fieldName) {

        FieldMeta meta = fieldMetaMap().get(fieldName);
        if (meta == null) {
            throw new IllegalArgumentException("字段不存在: " + fieldName);
        }
        return meta;
    }

    private Map<String, FieldMeta> fieldMetaMap() {

        return FIELD_META_CACHE.computeIfAbsent(impl.getEntityClass(), UpdateBuilder::scanFieldMeta);
    }

    private static Map<String, FieldMeta> scanFieldMeta(Class<?> entityClass) {

        Map<String, FieldMeta> map = new ConcurrentHashMap<>();
        for (Class<?> current = entityClass; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                ReflectionUtils.makeAccessible(field);
                map.putIfAbsent(
                        field.getName(),
                        new FieldMeta(field.getName(), field.getType(), field.isAnnotationPresent(Encrypt.class))
                );
            }
        }
        return map;
    }

    private record FieldMeta(String fieldName, Class<?> fieldType, boolean encrypt) {
    }

    private record Assignment(FieldMeta meta, Object value) {
    }

}
