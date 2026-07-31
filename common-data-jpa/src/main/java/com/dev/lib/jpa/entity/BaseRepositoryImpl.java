package com.dev.lib.jpa.entity;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.delete.CascadeSoftDeleteSupport;
import com.dev.lib.jpa.entity.query.QueryReadSupport;
import com.dev.lib.jpa.entity.write.RepositoryWriteContext;
import com.dev.lib.jpa.entity.write.RepositoryWritePluginChain;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
public class BaseRepositoryImpl<T extends JpaEntity> extends SimpleJpaRepository<T, Long>
        implements BaseRepository<T> {

    private static final int DEFAULT_JDBC_BATCH_SIZE = 128;

    private static final String JDBC_BATCH_SIZE_PROPERTY = "hibernate.jdbc.batch_size";

    private final EntityManagerFactory entityManagerFactory;

    private final EntityManager entityManager;

    private final Class<T> entityClass;

    private final JPAQueryFactory queryFactory;

    private final EntityPath<T> path;

    private final PathBuilder<T> pathBuilder;

    private final NumberPath<Long> deletedPath;

    private final NumberPath<Long> idPath;

    private final int jdbcBatchSize;

    public BaseRepositoryImpl(JpaEntityInformation<T, Long> entityInformation, EntityManager entityManager) {

        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityManagerFactory = entityManager.getEntityManagerFactory();
        this.entityClass = entityInformation.getJavaType();

        this.path = SimpleEntityPathResolver.INSTANCE.createPath(entityClass);
        this.pathBuilder = new PathBuilder<>(path.getType(), path.getMetadata());
        this.deletedPath = pathBuilder.getNumber("deleted", Long.class);
        this.idPath = pathBuilder.getNumber("id", Long.class);
        this.jdbcBatchSize = resolveConfiguredBatchSize(entityManager);
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Optional<T> load(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "load");
        return QueryReadSupport.load(this, null, dslQuery, expressions);
    }

    @Override
    public List<T> loads(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "loads");
        return QueryReadSupport.loads(this, dslQuery, expressions);
    }

    @Override
    public Page<T> page(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "page");
        return QueryReadSupport.page(this, dslQuery, expressions);
    }

    @Override
    public Optional<T> loadForUpdate(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "load");
        return QueryReadSupport.load(this, LockModeType.PESSIMISTIC_WRITE, dslQuery, expressions);
    }

    @Override
    public UpdateBuilder<T> update() {

        return new UpdateBuilder<>(this);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long delete(DslQuery<T> dslQuery, BooleanExpression... expressions) {

        ensureNonAggregateQuery(dslQuery, "delete");
        return CascadeSoftDeleteSupport.delete(this, dslQuery, expressions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <S extends T> @NonNull S save(@NonNull S entity) {

        RepositoryWriteContext<T> context = RepositoryWriteContext.from(this);
        return RepositoryWritePluginChain.getInstance()
                .resolve(context)
                .map(plugin -> plugin.save(context, entity))
                .orElseGet(() -> super.save(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <S extends T> @NonNull List<S> saveAll(@NonNull Iterable<S> entities) {

        RepositoryWriteContext<T> context = RepositoryWriteContext.from(this);
        return RepositoryWritePluginChain.getInstance()
                .resolve(context)
                .map(plugin -> plugin.saveAll(context, entities))
                .orElseGet(() -> saveAllInBatches(entities));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(@NonNull T entity) {

        CascadeSoftDeleteSupport.deleteEntity(this, entity);
    }

    private <S extends T> List<S> saveAllInBatches(Iterable<S> entities) {

        Objects.requireNonNull(entities, "entities must not be null");
        List<S> saved = new ArrayList<>();
        List<S> managedBatch = new ArrayList<>(jdbcBatchSize);

        for (S entity : entities) {
            Objects.requireNonNull(entity, "entities must not contain null");
            S managed;
            if (entity.isNew()) {
                entityManager.persist(entity);
                managed = entity;
            } else {
                managed = entityManager.merge(entity);
            }
            saved.add(managed);
            managedBatch.add(managed);

            if (managedBatch.size() == jdbcBatchSize) {
                flushAndDetach(managedBatch);
            }
        }

        if (!managedBatch.isEmpty()) {
            flushAndDetach(managedBatch);
        }
        return saved;
    }

    private void flushAndDetach(List<? extends T> entities) {

        entityManager.flush();
        entities.forEach(entityManager::detach);
        entities.clear();
    }

    private static int resolveConfiguredBatchSize(EntityManager entityManager) {

        Object configured = entityManager.getEntityManagerFactory().getProperties().get(JDBC_BATCH_SIZE_PROPERTY);
        if (configured == null) {
            return DEFAULT_JDBC_BATCH_SIZE;
        }

        try {
            int value = configured instanceof Number number
                    ? number.intValue()
                    : Integer.parseInt(configured.toString());
            return Math.max(1, value);
        } catch (RuntimeException ignored) {
            return DEFAULT_JDBC_BATCH_SIZE;
        }
    }

    private void ensureNonAggregateQuery(DslQuery<T> dslQuery, String operation) {

        if (dslQuery != null && dslQuery.hasAgg()) {
            throw new IllegalStateException("检测到 agg() 聚合配置，" + operation + " 不支持聚合查询");
        }
    }
}
