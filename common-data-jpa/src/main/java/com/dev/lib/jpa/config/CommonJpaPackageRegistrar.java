package com.dev.lib.jpa.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 将 com.dev.lib 追加到 Spring Boot 自动扫描包列表，
 * 使 JPA 自动配置能同时扫到 common-lib 和用户项目的 Entity/Repository。
 */
public class CommonJpaPackageRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String BASE_PACKAGE = "com.dev.lib";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

        AutoConfigurationPackages.register(registry, BASE_PACKAGE);
    }
}
