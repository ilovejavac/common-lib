package com.dev.lib.jpa.multiple;

import com.dev.lib.jpa.config.SingleDatasourceOnlyHibernatePropertiesCustomizer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernateSettings;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * 让多数据源场景与 Spring Boot 默认 JPA/Hibernate 属性解析保持一致。
 * 包括：
 * 1) spring.jpa.properties.*
 * 2) spring.jpa.hibernate.*（ddl-auto / naming 等）
 * 3) HibernatePropertiesCustomizer 扩展
 */
public class JpaHibernatePropertiesFactoryBean
        implements FactoryBean<Properties>, EnvironmentAware, BeanFactoryAware {

    private Environment environment;
    private BeanFactory  beanFactory;
    private Properties   cached;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Properties getObject() {

        if (cached == null) {
            cached = buildHibernateProperties();
        }
        return cached;
    }

    @Override
    public Class<?> getObjectType() {
        return Properties.class;
    }

    private Properties buildHibernateProperties() {

        Binder binder = Binder.get(environment);

        JpaProperties jpaProperties = binder.bind("spring.jpa", Bindable.of(JpaProperties.class))
                .orElseGet(JpaProperties::new);
        HibernateProperties hibernateProperties = binder
                .bind("spring.jpa.hibernate", Bindable.of(HibernateProperties.class))
                .orElseGet(HibernateProperties::new);

        HibernateSettings settings = new HibernateSettings()
                .ddlAuto(() -> environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .hibernatePropertiesCustomizers(resolveCustomizers());

        Map<String, Object> resolved = hibernateProperties
                .determineHibernateProperties(jpaProperties.getProperties(), settings);

        Properties result = new Properties();
        result.putAll(resolved);
        return result;
    }

    private Collection<HibernatePropertiesCustomizer> resolveCustomizers() {

        if (!(beanFactory instanceof ListableBeanFactory listableBeanFactory)) {
            return Collections.emptyList();
        }
        return listableBeanFactory.getBeansOfType(HibernatePropertiesCustomizer.class)
                .values()
                .stream()
                .filter(customizer -> !(customizer instanceof SingleDatasourceOnlyHibernatePropertiesCustomizer))
                .toList();
    }
}
