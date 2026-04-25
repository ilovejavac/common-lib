package com.dev.lib.jpa.entity.write;

import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import com.dev.lib.jpa.multiple.JpaDialect;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

public record RepositoryWriteContext<T extends JpaEntity>(
        BaseRepositoryImpl<T> repository,
        Class<T> entityClass,
        EntityManager entityManager,
        EntityManagerFactory entityManagerFactory,
        String datasourceName,
        JpaDialect logicalDialect
) {

    public static final String DATASOURCE_NAME_PROPERTY = "common.jpa.datasource-name";

    public static final String LOGICAL_DIALECT_PROPERTY = "common.jpa.logical-dialect";

    public static final String DEFAULT_DATASOURCE_NAME = "dataSource";

    public static <T extends JpaEntity> RepositoryWriteContext<T> from(BaseRepositoryImpl<T> repository) {

        EntityManagerFactory entityManagerFactory = repository.getEntityManagerFactory();
        Map<String, Object> properties = entityManagerFactory.getProperties();
        return new RepositoryWriteContext<>(
                repository,
                repository.getEntityClass(),
                repository.getEntityManager(),
                entityManagerFactory,
                resolveDatasourceName(properties),
                resolveLogicalDialect(properties)
        );
    }

    private static String resolveDatasourceName(Map<String, Object> properties) {

        Object value = properties.get(DATASOURCE_NAME_PROPERTY);
        return value == null ? DEFAULT_DATASOURCE_NAME : value.toString();
    }

    private static JpaDialect resolveLogicalDialect(Map<String, Object> properties) {

        Object value = properties.get(LOGICAL_DIALECT_PROPERTY);
        if (value instanceof JpaDialect dialect) {
            return dialect;
        }
        if (value == null || value.toString().isBlank()) {
            return JpaDialect.AUTO;
        }
        try {
            return JpaDialect.valueOf(value.toString());
        } catch (IllegalArgumentException ignored) {
            return JpaDialect.AUTO;
        }
    }
}
