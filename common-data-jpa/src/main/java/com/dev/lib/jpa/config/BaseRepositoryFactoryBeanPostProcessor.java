package com.dev.lib.jpa.config;

import com.dev.lib.jpa.entity.BaseRepository;
import com.dev.lib.jpa.entity.BaseRepositoryImpl;
import com.dev.lib.jpa.entity.JpaEntity;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.core.ResolvableType;

public class BaseRepositoryFactoryBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        if (!(bean instanceof JpaRepositoryFactoryBean<?, ?, ?> repositoryFactoryBean)) {
            return bean;
        }

        Class<?> repositoryInterface = repositoryFactoryBean.getObjectType();
        if (shouldUseBaseRepositoryImpl(repositoryInterface)) {
            repositoryFactoryBean.setRepositoryBaseClass(BaseRepositoryImpl.class);
        }

        return bean;
    }

    private boolean shouldUseBaseRepositoryImpl(Class<?> repositoryInterface) {

        if (repositoryInterface == null) {
            return false;
        }
        if (BaseRepository.class.isAssignableFrom(repositoryInterface)) {
            return true;
        }
        Class<?> domainType = ResolvableType.forClass(repositoryInterface)
                .as(Repository.class)
                .getGeneric(0)
                .resolve();
        return domainType != null && JpaEntity.class.isAssignableFrom(domainType);
    }
}
