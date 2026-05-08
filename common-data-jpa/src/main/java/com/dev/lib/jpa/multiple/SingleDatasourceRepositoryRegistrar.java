package com.dev.lib.jpa.multiple;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers repositories for the regular single-datasource path with the same
 * repository source used by {@link JpaDatasourceRegistrar}.
 */
public class SingleDatasourceRepositoryRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware, BeanFactoryAware {

    private static final String[] INTERNAL_REPOSITORY_PACKAGES = {"com.dev.lib.jpa.entity.log"};
    private static final String ENTITY_MANAGER_FACTORY_REF = "entityManagerFactory";
    private static final String TRANSACTION_MANAGER_REF = "transactionManager";

    private Environment environment;
    private ResourceLoader resourceLoader;
    private BeanFactory beanFactory;

    @Override
    public void setEnvironment(@NonNull Environment environment) {

        this.environment = environment;
    }

    @Override
    public void setResourceLoader(@NonNull ResourceLoader resourceLoader) {

        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {

        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata metadata,
                                        @NonNull BeanDefinitionRegistry registry) {

        if (hasManagedDatasourceGroup(registry)) {
            return;
        }
        if (!AutoConfigurationPackages.has(beanFactory)) {
            return;
        }
        List<String> packages = new ArrayList<>(AutoConfigurationPackages.get(beanFactory));
        packages.addAll(List.of(INTERNAL_REPOSITORY_PACKAGES));
        String[] applicationPackages = repositoryPackages(packages);
        registerRepositories(registry, applicationPackages, true);
    }

    private String[] repositoryPackages(List<String> packages) {

        List<String> selected = new ArrayList<>();
        packages.stream()
                .distinct()
                .sorted((left, right) -> Integer.compare(left.length(), right.length()))
                .forEach(packageName -> {
                    if (selected.stream().noneMatch(selectedPackage -> isSameOrSubPackage(packageName, selectedPackage))) {
                        selected.add(packageName);
                    }
                });
        return selected.toArray(String[]::new);
    }

    private boolean isSameOrSubPackage(String packageName, String candidateParent) {

        return packageName.equals(candidateParent) || packageName.startsWith(candidateParent + ".");
    }

    private void registerRepositories(BeanDefinitionRegistry registry,
                                      String[] packages,
                                      boolean considerNestedRepositories) {

        if (packages.length == 0) {
            return;
        }
        RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
        SimpleRepositoryConfigurationSource source = new SimpleRepositoryConfigurationSource(
                packages,
                ENTITY_MANAGER_FACTORY_REF,
                TRANSACTION_MANAGER_REF,
                considerNestedRepositories,
                resourceLoader,
                environment,
                registry
        );
        RepositoryConfigurationDelegate delegate =
                new RepositoryConfigurationDelegate(source, resourceLoader, environment);

        delegate.registerRepositoriesIn(registry, extension);
    }

    private boolean hasManagedDatasourceGroup(BeanDefinitionRegistry registry) {

        for (String beanName : registry.getBeanDefinitionNames()) {
            if (beanName.startsWith(JpaDatasourceRegistrar.MANAGED_DATASOURCE_GROUP_BEAN_PREFIX)) {
                return true;
            }
        }
        return false;
    }
}
