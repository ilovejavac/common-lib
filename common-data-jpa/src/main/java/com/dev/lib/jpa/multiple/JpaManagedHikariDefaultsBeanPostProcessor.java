package com.dev.lib.jpa.multiple;

import com.dev.lib.jpa.config.JpaHikariDefaultsProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import javax.sql.DataSource;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

@Slf4j
public class JpaManagedHikariDefaultsBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    private static final HikariDefaults HIKARI_DEFAULTS = createHikariDefaults();

    private final ObjectProvider<JpaManagedDatasourceGroup> managedDatasourceGroups;
    private final ObjectProvider<JpaHikariDefaultsProperties> hikariDefaultsProperties;
    private volatile Set<String> managedDatasourceNames;
    private volatile JpaHikariDefaultsProperties resolvedHikariDefaultsProperties;

    public JpaManagedHikariDefaultsBeanPostProcessor(
            ObjectProvider<JpaManagedDatasourceGroup> managedDatasourceGroups,
            ObjectProvider<JpaHikariDefaultsProperties> hikariDefaultsProperties
    ) {
        this.managedDatasourceGroups = managedDatasourceGroups;
        this.hikariDefaultsProperties = hikariDefaultsProperties;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (!(bean instanceof DataSource)) {
            return bean;
        }
        if (!resolveManagedDatasourceNames().contains(beanName)) {
            return bean;
        }
        if (!(bean instanceof HikariDataSource hikariDataSource)) {
            log.warn("Skip applying app.jpa.hikari to non-Hikari datasource bean [{}]: {}", beanName, bean.getClass().getName());
            return bean;
        }

        applyMaximumPoolSize(hikariDataSource);
        applyMinimumIdle(hikariDataSource);
        applyConnectionTimeout(hikariDataSource);
        applyValidationTimeout(hikariDataSource);
        applyIdleTimeout(hikariDataSource);
        applyMaxLifetime(hikariDataSource);
        applyKeepaliveTime(hikariDataSource);
        return bean;
    }

    @Override
    public int getOrder() {

        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void applyMaximumPoolSize(HikariDataSource hikariDataSource) {

        JpaHikariDefaultsProperties properties = resolveHikariDefaultsProperties();
        applyIfDefault(
                hikariDataSource.getMaximumPoolSize(),
                HIKARI_DEFAULTS.maximumPoolSize(),
                properties.getMaximumPoolSize(),
                hikariDataSource::setMaximumPoolSize
        );
    }

    private void applyMinimumIdle(HikariDataSource hikariDataSource) {

        JpaHikariDefaultsProperties properties = resolveHikariDefaultsProperties();
        applyIfDefault(
                hikariDataSource.getMinimumIdle(),
                HIKARI_DEFAULTS.minimumIdle(),
                properties.getMinimumIdle(),
                hikariDataSource::setMinimumIdle
        );
    }

    private void applyConnectionTimeout(HikariDataSource hikariDataSource) {

        JpaHikariDefaultsProperties properties = resolveHikariDefaultsProperties();
        applyIfDefault(
                hikariDataSource.getConnectionTimeout(),
                HIKARI_DEFAULTS.connectionTimeout(),
                properties.getConnectionTimeout(),
                hikariDataSource::setConnectionTimeout
        );
    }

    private void applyValidationTimeout(HikariDataSource hikariDataSource) {

        JpaHikariDefaultsProperties properties = resolveHikariDefaultsProperties();
        applyIfDefault(
                hikariDataSource.getValidationTimeout(),
                HIKARI_DEFAULTS.validationTimeout(),
                properties.getValidationTimeout(),
                hikariDataSource::setValidationTimeout
        );
    }

    private void applyIdleTimeout(HikariDataSource hikariDataSource) {

        JpaHikariDefaultsProperties properties = resolveHikariDefaultsProperties();
        applyIfDefault(
                hikariDataSource.getIdleTimeout(),
                HIKARI_DEFAULTS.idleTimeout(),
                properties.getIdleTimeout(),
                hikariDataSource::setIdleTimeout
        );
    }

    private void applyMaxLifetime(HikariDataSource hikariDataSource) {

        JpaHikariDefaultsProperties properties = resolveHikariDefaultsProperties();
        applyIfDefault(
                hikariDataSource.getMaxLifetime(),
                HIKARI_DEFAULTS.maxLifetime(),
                properties.getMaxLifetime(),
                hikariDataSource::setMaxLifetime
        );
    }

    private void applyKeepaliveTime(HikariDataSource hikariDataSource) {

        JpaHikariDefaultsProperties properties = resolveHikariDefaultsProperties();
        applyIfDefault(
                hikariDataSource.getKeepaliveTime(),
                HIKARI_DEFAULTS.keepaliveTime(),
                properties.getKeepaliveTime(),
                hikariDataSource::setKeepaliveTime
        );
    }

    private Set<String> resolveManagedDatasourceNames() {

        Set<String> names = managedDatasourceNames;
        if (names != null) {
            return names;
        }
        synchronized (this) {
            if (managedDatasourceNames == null) {
                managedDatasourceNames = managedDatasourceGroups.orderedStream()
                        .flatMap(group -> group.getDatasourceBeanNames().stream())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
            }
            return managedDatasourceNames;
        }
    }

    private JpaHikariDefaultsProperties resolveHikariDefaultsProperties() {

        JpaHikariDefaultsProperties properties = resolvedHikariDefaultsProperties;
        if (properties != null) {
            return properties;
        }
        synchronized (this) {
            if (resolvedHikariDefaultsProperties == null) {
                resolvedHikariDefaultsProperties = hikariDefaultsProperties.getObject();
            }
            return resolvedHikariDefaultsProperties;
        }
    }

    private static void applyIfDefault(int currentValue, int defaultValue, Integer configuredValue, IntConsumer setter) {

        if (configuredValue != null && currentValue == defaultValue) {
            setter.accept(configuredValue);
        }
    }

    private static void applyIfDefault(long currentValue, long defaultValue, Long configuredValue, LongConsumer setter) {

        if (configuredValue != null && currentValue == defaultValue) {
            setter.accept(configuredValue);
        }
    }

    private static HikariDefaults createHikariDefaults() {

        HikariDataSource hikariDataSource = new HikariDataSource();
        try {
            return new HikariDefaults(
                    hikariDataSource.getMaximumPoolSize(),
                    hikariDataSource.getMinimumIdle(),
                    hikariDataSource.getConnectionTimeout(),
                    hikariDataSource.getValidationTimeout(),
                    hikariDataSource.getIdleTimeout(),
                    hikariDataSource.getMaxLifetime(),
                    hikariDataSource.getKeepaliveTime()
            );
        } finally {
            hikariDataSource.close();
        }
    }

    private record HikariDefaults(
            int maximumPoolSize,
            int minimumIdle,
            long connectionTimeout,
            long validationTimeout,
            long idleTimeout,
            long maxLifetime,
            long keepaliveTime
    ) {
    }
}
