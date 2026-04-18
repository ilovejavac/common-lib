package com.dev.lib.jpa.multiple;

import com.dev.lib.jpa.config.JpaHikariDefaultsProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

import java.util.Collection;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

@Slf4j
public class JpaManagedHikariDefaultsBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {

    private static final HikariDefaults HIKARI_DEFAULTS = createHikariDefaults();

    private final Set<String> managedDatasourceNames;
    private final JpaHikariDefaultsProperties hikariDefaultsProperties;

    public JpaManagedHikariDefaultsBeanPostProcessor(
            Collection<JpaManagedDatasourceGroup> managedDatasourceGroups,
            JpaHikariDefaultsProperties hikariDefaultsProperties
    ) {
        this.managedDatasourceNames = managedDatasourceGroups.stream()
                .flatMap(group -> group.getDatasourceBeanNames().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.hikariDefaultsProperties = hikariDefaultsProperties;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (!managedDatasourceNames.contains(beanName)) {
            return bean;
        }
        if (!(bean instanceof HikariDataSource hikariDataSource)) {
            log.warn("Skip applying app.jpa.hikari-defaults to non-Hikari datasource bean [{}]: {}", beanName, bean.getClass().getName());
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

        applyIfDefault(
                hikariDataSource.getMaximumPoolSize(),
                HIKARI_DEFAULTS.maximumPoolSize(),
                hikariDefaultsProperties.getMaximumPoolSize(),
                hikariDataSource::setMaximumPoolSize
        );
    }

    private void applyMinimumIdle(HikariDataSource hikariDataSource) {

        applyIfDefault(
                hikariDataSource.getMinimumIdle(),
                HIKARI_DEFAULTS.minimumIdle(),
                hikariDefaultsProperties.getMinimumIdle(),
                hikariDataSource::setMinimumIdle
        );
    }

    private void applyConnectionTimeout(HikariDataSource hikariDataSource) {

        applyIfDefault(
                hikariDataSource.getConnectionTimeout(),
                HIKARI_DEFAULTS.connectionTimeout(),
                hikariDefaultsProperties.getConnectionTimeout(),
                hikariDataSource::setConnectionTimeout
        );
    }

    private void applyValidationTimeout(HikariDataSource hikariDataSource) {

        applyIfDefault(
                hikariDataSource.getValidationTimeout(),
                HIKARI_DEFAULTS.validationTimeout(),
                hikariDefaultsProperties.getValidationTimeout(),
                hikariDataSource::setValidationTimeout
        );
    }

    private void applyIdleTimeout(HikariDataSource hikariDataSource) {

        applyIfDefault(
                hikariDataSource.getIdleTimeout(),
                HIKARI_DEFAULTS.idleTimeout(),
                hikariDefaultsProperties.getIdleTimeout(),
                hikariDataSource::setIdleTimeout
        );
    }

    private void applyMaxLifetime(HikariDataSource hikariDataSource) {

        applyIfDefault(
                hikariDataSource.getMaxLifetime(),
                HIKARI_DEFAULTS.maxLifetime(),
                hikariDefaultsProperties.getMaxLifetime(),
                hikariDataSource::setMaxLifetime
        );
    }

    private void applyKeepaliveTime(HikariDataSource hikariDataSource) {

        applyIfDefault(
                hikariDataSource.getKeepaliveTime(),
                HIKARI_DEFAULTS.keepaliveTime(),
                hikariDefaultsProperties.getKeepaliveTime(),
                hikariDataSource::setKeepaliveTime
        );
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
