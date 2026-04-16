package com.dev.lib.jpa;

import com.dev.lib.jpa.config.AppDialectProperties;
import com.dev.lib.jpa.config.BaseRepositoryFactoryBeanPostProcessor;
import com.dev.lib.jpa.config.CommonJpaPackageRegistrar;
import com.dev.lib.jpa.config.CommonJpaProperties;
import com.dev.lib.jpa.config.SingleDatasourceOnlyHibernatePropertiesCustomizer;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

@AutoConfiguration(before = DataJpaRepositoriesAutoConfiguration.class)
@Import(CommonJpaPackageRegistrar.class)
@EnableConfigurationProperties({CommonJpaProperties.class, AppDialectProperties.class})
public class CommonJpaAutoConfig {

    @Bean
    public static BaseRepositoryFactoryBeanPostProcessor baseRepositoryFactoryBeanPostProcessor() {

        return new BaseRepositoryFactoryBeanPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {

        return new JPAQueryFactory(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean(name = "commonJpaPropertiesHibernateCustomizer")
    public HibernatePropertiesCustomizer commonJpaPropertiesHibernateCustomizer(CommonJpaProperties properties) {

        return hibernateProperties -> hibernateProperties.put(
                "app.jpa.in-clause-batch-size",
                properties.getInClauseBatchSize()
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "singleDatasourceDialectHibernateCustomizer")
    public SingleDatasourceOnlyHibernatePropertiesCustomizer singleDatasourceDialectHibernateCustomizer(AppDialectProperties appDialectProperties) {

        return hibernateProperties -> {
            Object existingDialect = hibernateProperties.get("hibernate.dialect");
            String resolvedDatabasePlatform = appDialectProperties.getDialect().resolveDatabasePlatform(
                    appDialectProperties.getDatabasePlatform(),
                    existingDialect instanceof String ? (String) existingDialect : null
            );
            if (StringUtils.hasText(resolvedDatabasePlatform)) {
                hibernateProperties.put("hibernate.dialect", resolvedDatabasePlatform);
            }
        };
    }

}
