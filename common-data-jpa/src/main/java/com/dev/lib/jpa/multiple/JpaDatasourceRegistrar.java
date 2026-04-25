package com.dev.lib.jpa.multiple;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.dev.lib.jpa.config.AppDialectProperties;
import com.dev.lib.jpa.entity.write.RepositoryWriteContext;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 读取 {@link JpaDatasource} 注解，为每个数据源注册：
 * <ol>
 *   <li>LocalContainerEntityManagerFactoryBean — 绑定 DataSource + entity 包</li>
 *   <li>JpaTransactionManager — 绑定上述 EMF</li>
 *   <li>SharedEntityManagerBean — 给 JPAQueryFactory 使用</li>
 *   <li>JPAQueryFactory — 绑定共享 EntityManager</li>
 *   <li>basePackages 下所有 JPA Repository bean — 通过 Spring Data RepositoryConfigurationDelegate</li>
 * </ol>
 *
 * <p>第一个声明的数据源自动标记为 {@code @Primary}。
 * {@code com.dev.lib} 自动追加到第一个数据源的扫描包中，保证 common-lib 内置
 * entity/repository 始终可用。
 */
public class JpaDatasourceRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

    private static final String COMMON_LIB_PACKAGE = "com.dev.lib";
    private static final String JPA_PROPERTIES_BEAN = "commonJpaResolvedHibernateProperties";
    private static final String MANAGED_DATASOURCE_GROUP_BEAN_PREFIX = "commonJpaManagedDatasourceGroup#";

    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // ==================== 入口 ====================

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata metadata,
                                        @NonNull BeanDefinitionRegistry registry) {

        List<AnnotationAttributes> specs = resolveSpecs(metadata);
        if (specs.isEmpty()) return;

        registerManagedDatasourceGroup(metadata, registry, specs);

        JpaProperties jpaProperties = bindJpaProperties();
        AppDialectProperties appDialectProperties = bindAppDialectProperties();
        String globalDatabasePlatform = appDialectProperties.getDialect().resolveDatabasePlatform(
                appDialectProperties.getDatabasePlatform(),
                jpaProperties.getDatabasePlatform()
        );
        registerSharedHibernateProperties(registry);
        String[] mappingResources = resolveMappingResources(jpaProperties);

        boolean first = true;
        for (AnnotationAttributes spec : specs) {
            String dsRef = spec.getString("datasource");
            String[] packages = spec.getStringArray("packages");
            JpaDialect dialect = spec.getEnum("dialect");
            validateSpec(dsRef, packages);
            boolean explicitDialectConfigured = dialect != JpaDialect.AUTO;
            String resolvedDatabasePlatform = explicitDialectConfigured
                    ? dialect.resolveDatabasePlatform(null, jpaProperties.getDatabasePlatform())
                    : globalDatabasePlatform;
            JpaDialect logicalDialect = explicitDialectConfigured
                    ? dialect
                    : appDialectProperties.getDialect();

            boolean isPrimary = first;
            if (first) {
                packages = appendIfMissing(packages);
                first = false;
            }

            String emfName = dsRef + "EntityManagerFactory";
            String tmName = dsRef + "TransactionManager";
            String sharedEmName = dsRef + "SharedEntityManager";
            String qfName = dsRef + "JpaQueryFactory";
            String vendorAdapterName = dsRef + "JpaVendorAdapter";

            registerVendorAdapter(registry, vendorAdapterName, jpaProperties, resolvedDatabasePlatform, isPrimary);
            registerEntityManagerFactory(registry, emfName, dsRef, packages, mappingResources, vendorAdapterName, resolvedDatabasePlatform, logicalDialect, isPrimary);
            registerTransactionManager(registry, tmName, emfName, isPrimary);
            registerSharedEntityManager(registry, sharedEmName, emfName);
            registerQueryFactory(registry, qfName, sharedEmName, isPrimary);
            registerRepositories(registry, packages, emfName, tmName);
        }
    }

    // ==================== 注解解析 ====================

    private List<AnnotationAttributes> resolveSpecs(AnnotationMetadata metadata) {

        // 多个 @JpaDatasource → Java 自动包装成 Container
        Map<String, Object> container =
                metadata.getAnnotationAttributes(JpaDatasource.Container.class.getName());
        if (container != null) {
            AnnotationAttributes[] arr = (AnnotationAttributes[]) container.get("value");
            return arr != null ? Arrays.asList(arr) : Collections.emptyList();
        }

        // 单个 @JpaDatasource
        Map<String, Object> single =
                metadata.getAnnotationAttributes(JpaDatasource.class.getName());
        if (single != null) {
            return Collections.singletonList(AnnotationAttributes.fromMap(single));
        }

        return Collections.emptyList();
    }

    private void registerManagedDatasourceGroup(AnnotationMetadata metadata,
                                                BeanDefinitionRegistry registry,
                                                List<AnnotationAttributes> specs) {

        String beanName = MANAGED_DATASOURCE_GROUP_BEAN_PREFIX + metadata.getClassName();
        if (registry.containsBeanDefinition(beanName)) {
            return;
        }
        Set<String> datasourceBeanNames = new LinkedHashSet<>();
        for (AnnotationAttributes spec : specs) {
            datasourceBeanNames.add(spec.getString("datasource"));
        }
        registry.registerBeanDefinition(
                beanName,
                BeanDefinitionBuilder.rootBeanDefinition(JpaManagedDatasourceGroup.class)
                        .addConstructorArgValue(datasourceBeanNames)
                        .getBeanDefinition()
        );
    }

    // ==================== 共享 JpaVendorAdapter ====================

    private void registerVendorAdapter(BeanDefinitionRegistry registry,
                                       String beanName,
                                       JpaProperties jpaProperties,
                                       String resolvedDatabasePlatform,
                                       boolean primary) {

        if (registry.containsBeanDefinition(beanName)) return;

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.rootBeanDefinition(HibernateJpaVendorAdapter.class)
                        .addPropertyValue("showSql", jpaProperties.isShowSql())
                        .addPropertyValue("generateDdl", jpaProperties.isGenerateDdl());
        if (jpaProperties.getDatabase() != null) {
            builder.addPropertyValue("database", jpaProperties.getDatabase());
        }
        if (StringUtils.hasText(resolvedDatabasePlatform)) {
            builder.addPropertyValue("databasePlatform", resolvedDatabasePlatform);
        }
        builder.setPrimary(primary);

        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    private void registerSharedHibernateProperties(BeanDefinitionRegistry registry) {

        if (registry.containsBeanDefinition(JPA_PROPERTIES_BEAN)) return;

        registry.registerBeanDefinition(
                JPA_PROPERTIES_BEAN,
                BeanDefinitionBuilder.rootBeanDefinition(JpaHibernatePropertiesFactoryBean.class)
                        .getBeanDefinition());
    }

    // ==================== EntityManagerFactory ====================

    private void registerEntityManagerFactory(BeanDefinitionRegistry registry,
                                              String beanName,
                                              String dsRef,
                                              String[] packages,
                                              String[] mappingResources,
                                              String vendorAdapterRef,
                                              String resolvedDatabasePlatform,
                                              JpaDialect logicalDialect,
                                              boolean primary) {

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.rootBeanDefinition(LocalContainerEntityManagerFactoryBean.class)
                        .addPropertyReference("dataSource", dsRef)
                        .addPropertyValue("persistenceUnitName", dsRef)
                        .addPropertyValue("packagesToScan", packages)
                        .addPropertyReference("jpaVendorAdapter", vendorAdapterRef)
                        .addPropertyReference("jpaProperties", JPA_PROPERTIES_BEAN);
        if (mappingResources != null && mappingResources.length > 0) {
            builder.addPropertyValue("mappingResources", mappingResources);
        }
        Map<String, Object> jpaPropertyMap = new HashMap<>();
        jpaPropertyMap.put(RepositoryWriteContext.DATASOURCE_NAME_PROPERTY, dsRef);
        jpaPropertyMap.put(RepositoryWriteContext.LOGICAL_DIALECT_PROPERTY, logicalDialect.name());
        if (StringUtils.hasText(resolvedDatabasePlatform)) {
            jpaPropertyMap.put("hibernate.dialect", resolvedDatabasePlatform);
        }
        builder.addPropertyValue("jpaPropertyMap", jpaPropertyMap);

        builder.setPrimary(primary);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    // ==================== TransactionManager ====================

    private void registerTransactionManager(BeanDefinitionRegistry registry,
                                            String beanName,
                                            String emfRef,
                                            boolean primary) {

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.rootBeanDefinition(JpaTransactionManager.class)
                        .addConstructorArgReference(emfRef);

        builder.setPrimary(primary);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    // ==================== SharedEntityManager（JPAQueryFactory 用）====================

    private void registerSharedEntityManager(BeanDefinitionRegistry registry,
                                             String beanName,
                                             String emfRef) {

        registry.registerBeanDefinition(
                beanName,
                BeanDefinitionBuilder.rootBeanDefinition(SharedEntityManagerBean.class)
                        .addPropertyReference("entityManagerFactory", emfRef)
                        .getBeanDefinition());
    }

    // ==================== JPAQueryFactory ====================

    private void registerQueryFactory(BeanDefinitionRegistry registry,
                                      String beanName,
                                      String sharedEmRef,
                                      boolean primary) {

        BeanDefinitionBuilder builder =
                BeanDefinitionBuilder.rootBeanDefinition(JPAQueryFactory.class)
                        .addConstructorArgReference(sharedEmRef);

        builder.setPrimary(primary);
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    // ==================== Repository 注册 ====================

    /**
     * 使用 Spring Data 的 {@link RepositoryConfigurationDelegate} 扫描并注册 Repository，
     * 与 {@code @EnableJpaRepositories} 走同一套机制，保证 baseClass 替换、fragment 等特性生效。
     */
    private void registerRepositories(BeanDefinitionRegistry registry,
                                      String[] packages,
                                      String emfRef,
                                      String tmRef) {

        RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();

        SimpleRepositoryConfigurationSource source =
                new SimpleRepositoryConfigurationSource(packages, emfRef, tmRef, resourceLoader, environment, registry);

        RepositoryConfigurationDelegate delegate =
                new RepositoryConfigurationDelegate(source, resourceLoader, environment);

        delegate.registerRepositoriesIn(registry, extension);
    }

    // ==================== 工具方法 ====================

    private JpaProperties bindJpaProperties() {

        return Binder.get(environment)
                .bind("spring.jpa", Bindable.of(JpaProperties.class))
                .orElseGet(JpaProperties::new);
    }

    private AppDialectProperties bindAppDialectProperties() {

        return Binder.get(environment)
                .bind("app", Bindable.of(AppDialectProperties.class))
                .orElseGet(AppDialectProperties::new);
    }

    private static void validateSpec(String dsRef, String[] packages) {

        if (!StringUtils.hasText(dsRef)) {
            throw new IllegalArgumentException("@JpaDatasource.datasource must not be empty");
        }
        if (packages == null || packages.length == 0) {
            throw new IllegalArgumentException("@JpaDatasource.packages must not be empty");
        }
    }

    private static String[] resolveMappingResources(JpaProperties jpaProperties) {

        if (jpaProperties.getMappingResources().isEmpty()) {
            return null;
        }
        return jpaProperties.getMappingResources().toArray(String[]::new);
    }

    private static String[] appendIfMissing(String[] arr) {

        for (String s : arr) {
            if (s.equals(JpaDatasourceRegistrar.COMMON_LIB_PACKAGE)) return arr;
        }
        String[] result = Arrays.copyOf(arr, arr.length + 1);
        result[arr.length] = JpaDatasourceRegistrar.COMMON_LIB_PACKAGE;
        return result;
    }
}
