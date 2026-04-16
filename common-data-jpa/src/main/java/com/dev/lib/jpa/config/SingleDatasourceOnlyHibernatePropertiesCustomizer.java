package com.dev.lib.jpa.config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;

/**
 * Marker interface for customizers that should only apply to the default
 * Spring Boot single-datasource JPA pipeline.
 */
@FunctionalInterface
public interface SingleDatasourceOnlyHibernatePropertiesCustomizer extends HibernatePropertiesCustomizer {
}
