package com.dev.lib.jpa;

import com.dev.lib.jpa.config.AppDialectProperties;
import com.dev.lib.jpa.config.BaseRepositoryFactoryBeanPostProcessor;
import com.dev.lib.jpa.config.CommonJpaPackageRegistrar;
import com.dev.lib.jpa.config.FinalSlowQueryLoggingListener;
import com.dev.lib.jpa.config.SingleDatasourceOnlyHibernatePropertiesCustomizer;
import com.dev.lib.jpa.config.SlowQueryProperties;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

@AutoConfiguration(before = DataJpaRepositoriesAutoConfiguration.class)
@Import(CommonJpaPackageRegistrar.class)
@EnableConfigurationProperties({AppDialectProperties.class, SlowQueryProperties.class})
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

    @Bean
    @ConditionalOnProperty(prefix = "decorator.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "app.jpa.slow-query", name = "enabled", havingValue = "true", matchIfMissing = true)
    public QueryExecutionListener finalSlowQueryLoggingListener(SlowQueryProperties slowQueryProperties) {

        return new FinalSlowQueryLoggingListener(
                slowQueryProperties.getThreshold(),
                slowQueryProperties.getLoggerName()
        );
    }

}
