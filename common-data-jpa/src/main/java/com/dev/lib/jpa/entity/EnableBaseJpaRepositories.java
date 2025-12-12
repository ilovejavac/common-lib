package com.dev.lib.jpa.entity;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableJpaRepositories(
//        repositoryFactoryBeanClass = BaseRepositoryFactoryBean.class,
        repositoryBaseClass = BaseRepositoryImpl.class
)
public @interface EnableBaseJpaRepositories {

    @AliasFor(annotation = EnableJpaRepositories.class, attribute = "value")
    String[] value() default {};

    @AliasFor(annotation = EnableJpaRepositories.class, attribute = "basePackages")
    String[] basePackages() default {};

    @AliasFor(annotation = EnableJpaRepositories.class, attribute = "basePackageClasses")
    Class<?>[] basePackageClasses() default {};

}
