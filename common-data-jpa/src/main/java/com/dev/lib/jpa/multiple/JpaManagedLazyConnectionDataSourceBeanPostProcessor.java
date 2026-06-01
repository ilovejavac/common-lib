package com.dev.lib.jpa.multiple;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.Set;

public class JpaManagedLazyConnectionDataSourceBeanPostProcessor implements BeanPostProcessor, Ordered {

    private static final String CONNECTION_FETCH_PROPERTY = "spring.datasource.connection-fetch";
    private static final String LAZY_CONNECTION_FETCH = "lazy";

    private final ObjectProvider<JpaManagedDatasourceGroup> managedDatasourceGroups;
    private final Environment environment;
    private volatile Set<String> managedDatasourceNames;

    public JpaManagedLazyConnectionDataSourceBeanPostProcessor(
            ObjectProvider<JpaManagedDatasourceGroup> managedDatasourceGroups,
            Environment environment
    ) {
        this.managedDatasourceGroups = managedDatasourceGroups;
        this.environment = environment;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (!isLazyConnectionFetchEnabled()) {
            return bean;
        }
        if (!(bean instanceof DataSource dataSource)) {
            return bean;
        }
        if (!resolveManagedDatasourceNames().contains(beanName)) {
            return bean;
        }
        if (isLazyConnectionDataSource(dataSource)) {
            return bean;
        }
        return new LazyConnectionDataSourceProxy(dataSource);
    }

    @Override
    public int getOrder() {

        return Ordered.LOWEST_PRECEDENCE - 30;
    }

    private boolean isLazyConnectionFetchEnabled() {

        String connectionFetch = environment.getProperty(CONNECTION_FETCH_PROPERTY);
        return connectionFetch != null && LAZY_CONNECTION_FETCH.equalsIgnoreCase(connectionFetch.trim());
    }

    private boolean isLazyConnectionDataSource(DataSource dataSource) {

        if (dataSource instanceof LazyConnectionDataSourceProxy) {
            return true;
        }
        try {
            dataSource.unwrap(LazyConnectionDataSourceProxy.class);
            return true;
        } catch (Exception e) {
            return false;
        }
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
}
